# nf-plugin-generator-cli (Nextflow plugin generator)

A command-line utility to quickly scaffold Nextflow plugins.

## Overview

`nf-plugin-generator-cli` is a simple command-line tool designed to streamline the creation of new Nextflow plugins. 
It takes a plugin name and optional parameters to generate a basic plugin structure, saving you time and ensuring consistency.

## Requirements



## Usage

To generate a new Nextflow plugin, execute the following command:

```bash
nf-plugin-generator-cli <plugin-name> [options]
```

Replace `<plugin-name>` with the desired name for your new Nextflow plugin (e.g., `my-awesome-plugin`).

### Options

The following options can be used to customize the plugin generation:

-   `-p <package-name>` or `--package <package-name>`: Specifies the package name for the plugin.
    -   Default: `nextflow.hello`
    -   Example:
        ```bash
        nf-plugin-generator-cli my-plugin --package com.example
        ```

  -   `-d <output-directory>` or `--dir <output-directory>`: Specifies the directory where the plugin structure will be generated.
      -   Default: The current working directory.
      -   Example:
        ```bash
        nf-plugin-generator-cli another-plugin -dir /path/to/plugins
        ```

-   `-nf-version <nextflow-version>` or `--nf-version <nextflow-version>`: Specifies the default Nextflow version to be used in the plugin configuration.
    -   Default: The latest stable Nextflow version at the time of plugin generation.
    -   Example:
        ```bash
        nf-plugin-generator-cli data-tools --nf-version 23.10.1
        ```

## Examples

1.  Generate a plugin named `my-plugin` with default settings:

    ```bash
    nf-plugin-generator-cli my-plugin
    ```

    This will create a directory named `my-plugin` in the current directory with the default package `nextflow.hello` and the latest stable Nextflow version configured.

2.  Generate a plugin named `bio-pipeline` with a custom package and output directory:

    ```bash
    nf-plugin-generator-cli bio-pipeline --package org.biocompute --dir /home/user/nextflow-plugins
    ```

    This will create a directory named `bio-pipeline` in `/home/user/nextflow-plugins` with the package `org.biocompute` and the latest stable Nextflow version configured.


## Getting Started (for developers)

Java Development Kit (JDK) 21 (Graalvm) or higher is required

1.  **Clone the repository:**

```bash
git clone <repository-url>
cd nf-plugin-generator-cli
```

2.  **Build the project (using Gradle):**

```bash
./gradlew clean build nativeCompile
```

This will create an executable file in the `build/native/nativeCompile` directory (e.g., `build/native/nativeCompile/nf-plugin-generator-cli`).

3.  **Run the CLI:**

```bash
build/native/nativeCompile/nf-plugin-generator-cli <plugin-name> [options]
```

## Contributing

Contributions to the `nf-plugin-generator-cli` project are welcome! 

Please feel free to submit pull requests or open issues for bug fixes, feature requests, or suggestions.

## License

MIT License