# Keep source line numbers useful in crash reports; the mapping file is a CI artifact.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Library consumer rules handle Compose. Add only evidence-based project rules here.

# The platform inflates the input method by name from the manifest.
-keep class io.github.lxzrvi.rekey.ime.RekeyImeService { *; }

# Enum.valueOf is used when reading persisted appearance settings.
-keepclassmembers enum io.github.lxzrvi.rekey.data.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
