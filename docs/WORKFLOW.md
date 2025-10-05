# GIT Workflow

## Branches and Commits

    master
    o
    |           feature/cool-name
    o---------->o
    |           |
    o           o
    |           |
    o<---[PR]---o
    .
    .
    .           bugfix/bug-name
    o---------->o
    |           |
    o           o
    |           |
    o<---[PR]---o
    .
    .
    .

We are following a [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) inspired workflow. That means
we format all our commit messages as follows.

- `RNR-0815: allow provided config object to extend other configs`
- `RNR-0815: correct minor typos in code`
- `RNR-0815: add polish language`

The format looks like this:

```
RNR-<story-id>: <description>

[optional body]

[optional footer(s)]
```

Choose from the following list for commit types:

| Commit Type | Description                                                                                                 |
| ----------- | ----------------------------------------------------------------------------------------------------------- |
| feat        | A new feature                                                                                               |
| fix         | A bug fix                                                                                                   |
| docs        | Documentation only changes                                                                                  |
| style       | Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)      |
| refactor    | A code change that neither fixes a bug nor adds a feature                                                   |
| perf        | A code change that improves performance                                                                     |
| test        | Adding missing tests or correcting existing tests                                                           |
| build       | Changes that affect the build system or external dependencies (example scopes: gulp, broccoli, npm)         |
| ci          | Changes to our CI configuration files and scripts (example scopes: Travis, Circle, BrowserStack, SauceLabs) |
| chore       | Other changes that don't modify src or test files                                                           |
| revert      | Reverts a previous commit                                                                                   |

## Releases

We use "Semantic Versioning" `major.minor.patch` for releases

`master` branch should always be stable and deployable.

`release` branches are for new major/minor versions (patch=0), e.g. `1.1.0`, `1.2.0`, `2.0.0`

`hotfix` branches are for new patch versions, e. g. `1.2.1`, `1.2.2`, `1.2.3`

    develop       master                  release/1.0.x
    o             o                       o tag=1.0.0
    |             .                       .
    o             .                       .                                   // merge features -> dev during sprint
    |             .                       .
    o----[PR]---->o                       .                                   // merge dev -> master if approved after review
    |             .                       .
    o             .                       .
    |             o-----[PR]------------->o tag=1.0.1                         // increase patch number
    o             .                       .
    |             .         hotfix/name   .
    o             .         o<------------+
    |             .         |             .
    o----[PR]---->o         o             .
    |             .         |             .
    0             .         o----[PR]---->o tag=1.0.2                         // e.g. deploy on PROD
    |             .                       .
    o----[PR]---->o                       .                release/1.1.x      // prepare new minor release
    .             o-----[PR]------------------------------>o tag=1.1.0        // e.g. deploy on Q
    .             .                       .                .
    .             .                       .                .
    .             .         hotfix/name   .                .
    .             .         o<------------+                .
    .             .         |             .                .
    .             .         o             .                .
    .             .         |             .                .
    .             .         o----[PR]---->o tag=1.0.3      .                  // e.g. fix issue for PROD
    .             .                       .                .
    .             .                       .                .
