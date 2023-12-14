# Converting to Kotlin

The conversion of Java code to Kotlin is in progress. To contribute, please
follow these tips:
- If you are making changes to a Java class for your feature/fix PR, consider migrating that class to Kotlin in the same PR as a separate commit.
- If the class is too large for converting in the feature/fix PR, you can open a separate PR for that matter. This will help prevent breaking the focus of the feature/fix PR.
- Consider adding or updating unit tests. They can help avoid accidentally breaking anything while migrating to Kotlin.

## Commit Tips to Help in Review

### Check the "Extra commit for .java > .kt renames" option in Android Studio:

When converting a file from Java to Kotlin, ensure you enable this option in Android Studio. It allows the conversion to be a two-step process:
1. The first commit will involve a simple rename from `.java` to `.kt`.
2. The second commit will rename the `.kt` extension back to .java, followed by the actual commit.

**Reason:** Enabling this option helps the reviewer perform a diff on the second commit, showing precisely what changes occurred between the Java and Kotlin files. Otherwise, the PR will display a file deletion (`.java` file) and a file addition (`.kt` file), making it challenging for the reviewer to diff effectively.

### Consider adding nullability annotations before initiating automatic conversion:

Address the warnings of "Missing null annotation" before starting automatic conversion.

**Reason:** This makes automatic conversion to handle nullability instead of setting everything to nullable.

### Depend on automatic conversion and commit immediately:

When performing the conversion, rely on the automatic conversion tools and commit the changes promptly, even if the resulting Kotlin code doesn't compile. Subsequently, on another commit, you can refine the Kotlin code, making it more idiomatic or addressing any compilation issues, such as adding nullable checks (`!!` or `let`).

**Reason:** This approach informs the reviewer and other readers that the first commit involved an automated conversion without manual intervention. It helps establish that any code refinements occurred separately, providing clarity on the development process.

