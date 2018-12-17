-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn org.conscrypt.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn javax.swing.**
-dontwarn java.awt.**
-dontwarn org.mozilla.javascript.tools.shell.**
-dontwarn org.mozilla.javascript.tools.debugger.**
-dontwarn com.adobe.xmp.**
-dontnote org.apache.commons.codec.**
-dontwarn com.sun.jdi.**
-dontwarn java.applet.Applet
-dontwarn sun.misc.*
-dontwarn sun.nio.ch.FileChannelImpl
-dontwarn sun.reflect.ReflectionFactory

-keepnames class * implements java.io.Serializable
 
-keepclassmembers class * implements java.io.Serializable { 
    static final long serialVersionUID; 
    private static final java.io.ObjectStreamField[] serialPersistentFields; 
    !static !transient <fields>; 
    private void writeObject(java.io.ObjectOutputStream); 
    private void readObject(java.io.ObjectInputStream); 
    java.lang.Object writeReplace(); 
    java.lang.Object readResolve(); 
} 

-keepnames class * extends android.app.Service

-keep,includedescriptorclasses class ch.poole.android.numberpicker.library.Interface.LimitExceededListener
-keep,includedescriptorclasses class ch.poole.android.numberpicker.library.Interface.ValueChangedListener
-keep,includedescriptorclasses class ch.poole.rangebar.RangeBar$OnRangeBarChangeListener
-keep,includedescriptorclasses class ch.poole.rangebar.RangeBar$OnRangeBarTextListener
-keep,includedescriptorclasses class ch.poole.rangebar.IRangeBarFormatter
-keep,includedescriptorclasses class ch.poole.rangebar.RangeBar$PinTextFormatter
-keep,includedescriptorclasses class cn.carbswang.android.numberpickerview.library.NumberPickerView$OnScrollListener
-keep,includedescriptorclasses class cn.carbswang.android.numberpickerview.library.NumberPickerView$OnValueChangeListener
-keep,includedescriptorclasses class cn.carbswang.android.numberpickerview.library.NumberPickerView$OnValueChangeListenerRelativeToRaw
-keep,includedescriptorclasses class cn.carbswang.android.numberpickerview.library.NumberPickerView$OnValueChangeListenerInScrolling
-keep,includedescriptorclasses class com.buildware.widget.indeterm.IndeterminateRadioButton$OnStateChangedListener
-keep,includedescriptorclasses class com.afollestad.materialdialogs.GravityEnum
-keep,includedescriptorclasses class com.buildware.widget.indeterm.IndeterminateCheckBox$OnStateChangedListener
-keep,includedescriptorclasses class com.github.aakira.expandablelayout.ExpandableLayoutListener
-keep,includedescriptorclasses class org.acra.config.CoreConfiguration
-keep,includedescriptorclasses class org.acra.builder.ReportBuilder
-keep,includedescriptorclasses class org.acra.data.CrashReportData
-keep,includedescriptorclasses class org.acra.collections.ImmutableSet
-keep,includedescriptorclasses class org.acra.config.CoreConfigurationBuilder
-keep,includedescriptorclasses class org.acra.log.ACRALog
-keep,includedescriptorclasses class org.acra.sender.HttpSender$1
-keep,includedescriptorclasses class de.blau.android.BuildConfig
-keep,includedescriptorclasses class de.blau.android.osm.ViewBox
-keep,includedescriptorclasses class de.blau.android.presets.Preset$PresetItem
-keep,includedescriptorclasses class de.blau.android.util.StringWithDescription
-keep,includedescriptorclasses class de.blau.android.views.CustomAutoCompleteTextView$Tokenizer
-keep class de.blau.android.services.*

-keep,includedescriptorclasses class com.mapbox.geojson.** {
  public protected private *;
}

-keep class sun.misc.Unsafe { *; }

-keepattributes Signature
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.AutoValue$Builder
-dontnote com.google.gson.internal.UnsafeAllocator
-dontwarn javax.annotation.concurrent.GuardedBy
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
