# Text2Png

Convert lyrics text to transparent PNG images usable with, for example, OBS Studio.

Each chunk of whitespace separated text in an input text file is transformed into a .png file.

## Usage

```bash

$ sudo dnf install java-17-openjdk-devel.x86_64

$ java -version

$ javac Lyrics.java

$ java Lyrics --position bottom --percent 35 ./songs

$ ls -R ./songs/images
```



