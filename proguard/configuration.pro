-dontshrink
-dontoptimize
-useuniqueclassmembernames
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep class com.kbdunn.nimbus.server.Launcher {
	void main (java.lang.String[]);
}

-keep class com.kbdunn.nimbus.web.bean.** {
	public static <fields>;
	
    void set*(***);
    void set*(int, ***);

    boolean is*(); 
    boolean is*(int);

    *** get*();
    *** get*(int);
}

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    private static synthetic java.lang.Object $deserializeLambda$(java.lang.invoke.SerializedLambda);
}
-keepclassmembernames class * {
    private static synthetic *** lambda$*(...);
}

-keepattributes Signature

-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}