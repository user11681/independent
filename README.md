## download
from [releases](https://github.com/user11681/independent/releases)

## usage
### the normal way

Run `independent-${version}.jar` in your mod directory in order to remove dependency declarations from every mod in it.

### the terminal way

removing dependency declarations from mods in `/test directory/mods`:
```bash
java -jar independent-${version}.jar "/test directory/mods"
```

Multiple paths can be passed in a single run.
Using `-r` or `--recursive` causes mods to be searched for recursively starting in the specified directory.

removing dependency declarations from a `/test directory/mods/mod.jar`:
```bash
java -jar independent-${version}.jar "/test directory/mods/mod.jar"
```

