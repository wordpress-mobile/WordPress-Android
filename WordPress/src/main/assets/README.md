`supported-blocks.json` used to be a symlink of `libs/gutenberg-mobile/src/block-support/supported-blocks.json`.

As we work towards removing the dependency on the `gutenberg-mobile` submodule, it has been duplicated here, from the value it had at `v1.43.0`.

Once the migration from submodule to Bintray dependency + composite build has been completed, we might want to find a way to remove this duplication.
