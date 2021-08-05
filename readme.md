# MPXJ

Welcome to MPXJ! This library provides a set of facilities to allow project information to be manipulated in Java, .Net, Ruby and other languages. 
Full documentation including release notes can be found online at [http://mpxj.org](http://mpxj.org). 
# MPXJ文档整理 

---

### 介绍

* 该库基于一组数据结构，这些数据结构遵循 Microsoft Project 表示计划数据的方式。项目数据的所有操作都使用这些数据结构进行，这些数据结构可以从各种支持的文件格式中读取或写入。另外MpxjExtensionMethods扩展方法提供了JAVA与C#的类型转换。额外依赖IKVM-OpenJdk运行。底层基于Java实现、此版本for-Csharp版屏蔽了Java语法，去掉了getter、setter、改为属性。具体可参照官网Java代码片段 http://www.mpxj.org/

---

### 支持的格式

|  MPX  |  √   |
| :---: | :--: |
|  MPP  |  √   |
| MSPDI |  √   |
|  MPD  |  √   |

### 安装

*  Nuget  => 检索  MPXJ  =>  选择 for csharp 9.5.1版本

---

### 实体关系图

 ![关键 MPXJ 实体图](http://www.mpxj.org/images/mpxj-entities.png)
---

---

## 快速上手  >_

###  1.读取文件方式

``` c#
ProjectReader reader = new UniversalProjectReader ();// 使用通用加载器 自动识别文件类型
ProjectFile project = reader.read("C:\\Users\\zpf\\Documents\\TestDemoFile.mpp");
```

* 指定加载器：

```java
net.sf.mpxj.mpx.MPXWriter: // 写入 Microsoft MPX 文件
net.sf.mpxj.mspdi.MSPDIWriter: // 写入 Microsoft MSPDI (XML) 文件
net.sf.mpxj.primavera.PrimaveraPMFileWriter: // 写入 Primavera PMXML (XML) 文件
net.sf.mpxj.planner.PlannerWriter: // 写入规划器 (XML) 文件
net.sf.mpxj.json.JsonWriter: // 写入 JSON 文件（主要用于支持 Ruby 版本的 MPXJ）
```

### 2.写文件

```c#
ProjectWriter writer = new MPXWriter(); // 所有的类都实现了ProjectWriter接口 可以直接调用 写入器
writer.write(project, "C:\\Users\\zpf\\Documents\\TestDemoFile.mpp");
```



###  3.任务和资源读取

```java
ProjectReader reader = new UniversalProjectReader ();
ProjectFile project = reader.read("C:\\Users\\zpf\\Documents\\TestDemoFile.mpp");
var resources = project.Resources;
   foreach (Resource resource in resources.ToIEnumerable()){
           Console.WriteLine(resource.UniqueID); // 获取资源的唯一标识Id
  }
         
```

### 4.资源分配

* 任务和资源通过资源分配相关联。ProjectFile 类中有一个方法可以检索文件中的所有资源分配。下面的代码片段使用它来提供所有分配的概述。

```JAVA
   foreach (ResourceAssignment  assignment in project.ResourceAssignments.ToIEnumerable())
            {   // 遍历资源概述
                Task task = assignment.Task;
                String taskName;
                if (task == null)
                {
                    taskName = "NULL";
                }
                else
                {
                    taskName = task.Name;
                }

                Resource resource = assignment.Resource;

                String resourceName;
                if (resource == null)
                {
                    resourceName = "(null resource)";
                }
                else
                {
                    resourceName = resource.Name;
                }
                Console.WriteLine("Assignment: Task=" + taskName + " Resource=" + resourceName);
            }
```

* 也可以逐个任务地检索资源分配.

```JAVA
  ProjectReader reader = new UniversalProjectReader ();
  ProjectFile project = reader.read("C:\\Users\\zpf\\Documents\\TestDemoFile.mpp");
  foreach (Task task in tasks.ToIEnumerable()){
       foreach (ResourceAssignment resource in task.ResourceAssignments.ToIEnumerable())
         {
               Console.WriteLine(resource.Task.Active); // 活动 
               Console.WriteLine(resource.Task.Calendar); // 日历
               Console.WriteLine(resource.Task.Cost); // 花费
               Console.WriteLine(resource.Task.Deadline); // 截止日期
               Console.WriteLine(resource.Task.Start); // 任务开始时间
               Console.WriteLine(resource.Task.Finish); // 任务结束时间
               Console.WriteLine(resource.Units);// 资源的单位
        }
```

  

### 5.日历

* 日历用于定义工作和非工作时间，并且是定义为项目一部分的更复杂的结构之一。它们依次用于定义调度任务的时间段。有两种类型的日历：基本日历和资源日历。每个基准日历都提供了一周中每一天的工作和非工作时间的完整定义。资源日历与单个资源相关联。每个资源日历都源自一个基准日历；资源日历可能未经修改，在这种情况下，它看起来与基础日历相同，或者资源日历可能会修改工作日和非工作日。在这种情况下，这些更改会“叠加”在基准日历定义的工作和非工作时间之上

  ```JAVA
  var calendars = project.Calendars; 
  ```

* 还可以将特定日历与单个任务相关联。下面的方法调用显示了与正在检索的任务关联的日历。

  ```
  ProjectCalendar taskCalendar = project.Calendars; 
  ```

* 记住一个日历可能来自另一个日历，在选择日历实例上调用的方法时必须小心：一些方法用于检索仅定义为该特定日历的一部分的属性，而其他方法则用于向下遍历日历的层次结构，直到检索到“实际”值。例如，getDays 方法将检索指示当前日历定义的一周中每一天的工作/非工作/默认状态的标志数组。但是 getDay 方法将测试当前日历以查看它是工作日还是非工作日。如果当前日历中的标志设置为“默认”，则该方法将使用派生当前日历的基准日历来确定当天是工作还是非工作。

  如上所述，日历包含一组代表一周中每一天的标志，这些标志表示一周中的哪一天是非工作或“默认”。如果一天设置为“默认”，则该天的工作时间取自基础基准日历（如果它是资源日历），或者使用 Microsoft Project 提供的默认值（如果它是基准日历）。

  如果将某一天定义为工作日，则日历还将包含该天的一组工作时间。一天的工作时间由 ProjectCalendarHours 类的实例定义。这包含一组 DateRange 实例，这些实例定义了一天中每个工作时段的开始和结束时间。

  除了控制一天是工作还是非工作以及每天的工作时间的标志外，每个日历还定义了一组例外，用于“覆盖”个别天或整个天的默认工作或非工作时间日期范围。提供的方法允许检索日历定义的所有例外的列表，或检索涵盖单个日期的例外。日历异常由 ProjectCalendarException 类的实例表示。

### 6.时间分段数据

尽管资源分配本身描述了将哪些资源分配给哪些任务，以及它们将完成多少工作，但这并不一定告诉我们资源在任何特定日期将完成多少工作。为了找到此信息，您需要查阅时间分段资源分配数据。

每个资源分配都有一对方法允许您检索时间分段数据，如下面的示例代码所示。

```java
var planned = assignment.TimephasedOvertimeWork.ToIEnumerable(); // 分时加班
var complete = assignment.TimephasedActualWork.ToIEnumerable(); // 分时实际工时
```

* 有四个属性：开始日期、结束日期、总工作量和每天的工作量

```JAVA
[TimephasedItem start=Wed Aug 04 08:00:00 CST 2021 totalAmount=4.0h finish=Wed Aug 04 17:00:00 CST 2021 amountPerDay=4.0h modified=false]

```

* Tips 

  例如，您可以检索 TimephasedResourceAssignment 类的一个实例，其开始和结束日期定义了一个五天的时间段。该期间的总工作时间为 40 小时，每天的工作时间定义为 8 小时。这表示在相关期间，在日期范围内的每个工作日，将执行 8 小时的工作。重要的是要记住，非工作日被忽略，例如，如果我们有一个跨越周末的 7 天时间段，则总工作时间仍可能为 40 小时，每天工作 8 小时：仅 5 个工作日是分配的工作，非工作周末的工作时间为零。

  上面定义的两个列表将包含多个 TimephasedResourceAssignment 实例，其中在不同的日子工作不同的小时数。一天中工作相同小时数的每个连续日期范围将由一个 TimephasedResourceAssignment 实例表示。

  两个时间分段数据列表分别代表已完成（实际）工作和计划工作。如果这些列表用于显示部分完成的一天的工作，则这些列表可能会重叠一天。例如，在正常的 8 小时工作日中，如果已经完成了 4 小时的工作，还剩下 4 小时，那么已完成的时间分段数据列表将以 4 小时已完成的工作结束，计划工作列表将从当天剩余的 4 小时开始。

​		                                                                                                                                                                                                                                              @<u>zpf</u>



