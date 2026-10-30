# Keep source line numbers useful in crash reports; the mapping file is a CI artifact.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Library consumer rules handle Compose. Add only evidence-based project rules here.
