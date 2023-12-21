-printusage build/tmp/usage.txt 

-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.Nonnull
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn javax.annotation.meta.TypeQualifierDefault
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
-keep,includedescriptorclasses class ch.poole.android.checkbox.IndeterminateRadioButton$OnStateChangedListener
-keep,includedescriptorclasses class com.afollestad.materialdialogs.GravityEnum
-keep,includedescriptorclasses class ch.poole.android.checkbox.IndeterminateCheckBox$OnStateChangedListener
-keep,includedescriptorclasses class com.github.aakira.expandablelayout.ExpandableLayoutListener

-keep,includedescriptorclasses class org.acra.** { *; }
-keep class * implements org.acra.plugins.Plugin {*;}
-keep enum org.acra.** {*;}

-keep,includedescriptorclasses class de.blau.android.BuildConfig
-keep,includedescriptorclasses class de.blau.android.osm.ViewBox
-keep,includedescriptorclasses class de.blau.android.presets.Preset$PresetItem
-keep,includedescriptorclasses class de.blau.android.util.StringWithDescription
-keep,includedescriptorclasses class de.blau.android.views.CustomAutoCompleteTextView$Tokenizer
-keep class de.blau.android.services.*

-keep class org.mozilla.javascript.** { *; }
-keep class de.blau.android.util.GeoMath
-keep class de.blau.android.osm.BoundingBox
-keep class de.blau.android.osm.Logic

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
-keep class org.eclipse.egit.github.core.** { *; }
