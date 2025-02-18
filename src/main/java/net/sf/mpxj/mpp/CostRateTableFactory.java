/*
 * file:       CostRateTableFactory.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2009
 * date:       09/06/2009
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.mpp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sf.mpxj.CostRateTable;
import net.sf.mpxj.CostRateTableEntry;
import net.sf.mpxj.Rate;
import net.sf.mpxj.Resource;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.common.DateHelper;
import net.sf.mpxj.common.NumberHelper;

/**
 * Common code to read resource cost rate tables from MPP files.
 */
final class CostRateTableFactory
{
   /**
    * Creates a CostRateTable instance from a block of data.
    *
    * @param resource parent resource
    * @param index cost rate table index
    * @param data data block
    */
   public void process(Resource resource, int index, byte[] data)
   {
      List<CostRateTableEntry> entries = new ArrayList<>();
      Calendar cal = DateHelper.popCalendar();

      //
      // Extract core data
      //
      if (data == null)
      {
         //
         // MS Project economises by not actually storing the first cost rate
         // table if it doesn't need to, so we take this into account here.
         //
         if (index == 0)
         {
            Rate standardRate = resource.getStandardRate() == null ? new Rate(0, TimeUnit.HOURS) : resource.getStandardRate();
            TimeUnit standardRateUnits = standardRate.getUnits();

            Rate overtimeRate = resource.getOvertimeRate() == null ? new Rate(0, TimeUnit.HOURS) : resource.getOvertimeRate();
            TimeUnit overtimeRateUnits = overtimeRate.getUnits();

            Number costPerUse = resource.getCostPerUse() == null ? NumberHelper.DOUBLE_ZERO : resource.getCostPerUse();
            Date endDate = CostRateTableEntry.DEFAULT_ENTRY.getEndDate();

            entries.add(new CostRateTableEntry(standardRate, standardRateUnits, overtimeRate, overtimeRateUnits, costPerUse, null, endDate));
         }
         else
         {
            entries.add(CostRateTableEntry.DEFAULT_ENTRY);
         }
      }
      else
      {
         for (int i = 16; i + 44 <= data.length; i += 44)
         {
            Rate standardRate = new Rate(MPPUtility.getDouble(data, i), TimeUnit.HOURS);
            TimeUnit standardRateFormat = getFormat(MPPUtility.getShort(data, i + 8));
            Rate overtimeRate = new Rate(MPPUtility.getDouble(data, i + 16), TimeUnit.HOURS);
            TimeUnit overtimeRateFormat = getFormat(MPPUtility.getShort(data, i + 24));
            Double costPerUse = NumberHelper.getDouble(MPPUtility.getDouble(data, i + 32) / 100.0);
            Date endDate = MPPUtility.getTimestampFromTenths(data, i + 40);

            if (endDate.getTime() > DateHelper.END_DATE_NA.getTime())
            {
               endDate = DateHelper.END_DATE_NA;
            }
            else
            {
               //
               // MPP files only store the end date of the range, and typically this
               // will be represented as the last minute of the range, e,g, 07:59,
               // so the next range starts at 08:00. Occasionally we see the start time of the
               // next range stored here, so this heuristic is used to identify what looks
               // like a start time (minutes divisible by 10) and subtracts one minute so that
               // the next range starts at the correct time.
               //
               cal.setTime(endDate);
               int minutes = cal.get(Calendar.MINUTE);

               if ((minutes % 5) == 0)
               {
                  cal.add(Calendar.MINUTE, -1);
                  endDate = cal.getTime();
               }
            }
            entries.add(new CostRateTableEntry(standardRate, standardRateFormat, overtimeRate, overtimeRateFormat, costPerUse, null, endDate));
         }
      }

      //
      // Populate start dates
      //
      Collections.sort(entries);
      CostRateTable result = new CostRateTable();

      for (int i = 0; i < entries.size(); i++)
      {
         Date startDate;
         if (i == 0)
         {
            startDate = DateHelper.START_DATE_NA;
         }
         else
         {
            cal.setTime(entries.get(i - 1).getEndDate());
            cal.add(Calendar.MINUTE, 1);
            startDate = cal.getTime();
         }

         CostRateTableEntry entry = entries.get(i);
         result.add(new CostRateTableEntry(entry.getStandardRate(), entry.getStandardRateFormat(), entry.getOvertimeRate(), entry.getOvertimeRateFormat(), entry.getCostPerUse(), startDate, entry.getEndDate()));
      }

      resource.setCostRateTable(index, result);
   }

   /**
    * Converts an integer into a time format.
    *
    * @param format integer format value
    * @return TimeUnit instance
    */
   private TimeUnit getFormat(int format)
   {
      TimeUnit result;
      if (format == 0xFFFF)
      {
         result = TimeUnit.HOURS;
      }
      else
      {
         result = MPPUtility.getWorkTimeUnits(format);
      }
      return result;
   }
}
