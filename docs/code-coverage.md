# Code coverage

This project uses [Kover](https://github.com/Kotlin/kotlinx-kover) tool for generating code coverage metrics.
To run the code coverage report for the whole codebase, run `./gradlew koverHtmlReport` and open the HTML report at `WordPress/build/reports/kover/html/index.html`.

## Coverage exclusions

To get more precise results of the metrics, some classes are excluded from code coverage calculations. Those classes are e.g. Dagger/Hilt generated code, Acitivies, Fragments, databinding etc. A complete list of exclusions is available in the `config/gradle/code_coverage.gradle` file.

## Codecov

We also have `Codecov` integration for getting reports on each PR and observing trends. See [the dashboard](https://app.codecov.io/github/wordpress-mobile/WordPress-Android/) to get more insights.
