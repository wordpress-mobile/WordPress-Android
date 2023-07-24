# Converting to Kotlin

The conversion of our Java code to Kotlin is still in progress. To facilitate this process, please
follow these tips:
- If you are making changes to a Java class for your feature/fix PR, consider migrating that class to Kotlin in the same PR as a separate commit.
- If the class is too large for converting in the feature/fix PR, you can open a separate PR for the conversion the class. This will help prevent breaking the focus of the feature/fix PR.
- Consider adding or updating unit tests, which could help to avoid accidentally breaking anything while migrating to Kotlin.
