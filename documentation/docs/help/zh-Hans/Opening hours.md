# 开放街图营业时间编辑器

OpenStreetMap 营业时间规范相当复杂，并不易于一个简单直观的使用者介面。

然而大部分的时候，你应该只会用到一小部分的定义。编辑器尝试在选单中隐藏更加难懂的功能，来达成这一点，大部分的时候减少“路上”设定，尽可能用预先设定的模版，加以小量修改。

_本文档是早期文档，正在开发中_

## 使用开放街图编辑器

在一般的工作流程中，你编辑的物件一般已经有开放时间的标签 (opening_hour、service_times 和 colllection_times)，或是你可以重新设定物件的预设组合，用空白的开放时间栏位。如果你需要手动输入栏位，像是用 Vespucci，你可以在详情页面输入关键值，之后切换回栏位页签继续编辑。如果你相信开放时间标签必须多于预设组合的话，请在你的编辑器开启 issue。

如果你已经定议预设的模板 (经由"管理模板"选单项目)，会在编辑器的空白数值自动载入。使用"载入模板"功能，你可以载入任何已经储存的模板，然后用"储存模板"选单你可以储存目前的数值为模板。你可以定议不同的模板，预设"opening_hours"、"collection_times"和"service_times"标签。

当然，您可以从头开始构建营业时间的值，但我们建议使用现有的模板之一作为开始。

如果已经有营业时间的数值则会载入，另外会自动修正以符合营业时间规范。如果无法修正，则会在大概的位置显示错误讯息，原始的营业时间数值，等待人工修正。开放街图资料库中有大约1/4的营业时间数值有问题，但只有少于10%的状况是无法修正。详见 [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser)，并且看有那些允许的规范变体。

### 主菜单按键

* __添加规则__：添加新规则。
* __为假期添加规则__：当国家/地区改变时为假期添加新规则
* __为 24/7 增加规则__：为总是开放的物件增加规则，开放时间规范不支持任何其他子值为24/7，但我们允许增加更高阶的选择器 (例如年份范围)。
* __Load template__：加载现有模板。
* __保存到模板__：将当前的营业时间值另存为模板，以作为备份为将来使用。
* __管理模板__：编辑（例如更改名称）并删除现有模板。
* __刷新__：重新加载营业时间值。
* __全部删除__：删除所有规则

### 规则

默认规则被添加为_normal_规则，这意味着它们将在同一天覆盖先前规则的值。在指定延长时间时中，可能会出现一个问题，通常您随后会希望通过_Show rule type_菜单项将规则切换为_additive_。

#### 规则菜单

* __添加修饰符/注释__：更改此规则的效果并添加可选注释。
* __添加假期__：为公共或学校假期添加选择器。
* __增加时间跨度...__
    * __时间-时间__：同一天的开始时间到结束时间。
    * __时间-延长时间__：第二天的开始时间到结束时间（例如26:00是第二天的凌晨02:00）。
    * __变化时间-时间__：从开始可变时间（黎明，黄昏，日出和日落）到同一天的结束时间。
    * __变化时间-延长时间__：从开始变化时间到第二天的结束时间。
    * __时间-变化时间__：开始时间到结束变化时间。
    * __变化时间-变化时间__：从开始变化时间到结束变化时间。
    * __时间__：一个时间点。
    * __开放时间__：从某个时间点开始。
    * __变化时间__：特定的变化时间
    * __变化时间 - 一直持续__：从变化时间到一直持续
* __增加工作日范围__：添加基于工作日的选择器。
* __增加日期范围...__
    * __日期-日期__：从开始日期（年，月，日）到结束日期。
    * __变化日期-日期__：从开变化变日期（当前规范仅定义_easter_）到结束日期。
    * __日期-变化日期__：从开始日期到变化日期
    * __变化日期-变化日期__：从开始变化日期到结束变化日期
    * __每月发生-每月发生__：一个月中从开始的周间是相同的。
    * __日期- 每月发生__：一个月中周间发生，到特定日期结束
    * __日期-发生在月份__：从开始日舒到一个月中的周间结束。
    * __Occurrence in month - variable date__: from a start weekday occurrence in a month to an end variable date.
    * __Variable date - occurrence in month__: from a start variable date to an end weekday occurrence in a month.
    * __Date - open end__: from a start date onwards.
    * __Variable date - open end__: from a start variable date onwards.
    * __Occurrence in month - open end__: from a start weekday occurrence in a month onwards.
    * __With offsets...__: the same entries as above however with offsets specified (this is rarely used).
* __Add year range__: add a year based selector.
* __Add week range__: add a week number based selector.
* __Duplicate__: create a copy of this rule and insert it after the current position.
* __Show rule type__: display and allow changing of the rule type _normal_, _additive_ and _fallback_ (not available on the first rule).
* __Move up__: move this rule up one position (not available on the first rule).
* __Move down__: move this rule down one position.
* __Delete__: delete this rule.

### Time spans

To make editing time spans as easy as possible, we try to choose an optimal time range and granularity for the range bars when loading existing values. For new time spans the bars start at 6:00 (am) and have 15 minute increments, this can be changed via the menu.

Clicking (not on the pins) the time bar will open the large time picker, when using the bars directly is too difficult. The time pickers extend in to the next day, so they are a simple way to extend a time range without having to delete and re-add the the range.

#### Time span menu

* __Display time picker__: show a large time picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __Switch to 15 minute ticks__: use 15 minute granularity for the range bar.
* __Switch to 5 minute ticks__: use 5 minute granularity for the range bar.
* __Switch to 1 minute ticks__: use 1 minute granularity for the range bar, very difficult to use on a phone.
* __Start at midnight__: start the range bar at midnight.
* __Show interval__: show the interval field for specifying an interval in minutes.
* __Delete__: delete this time span.

