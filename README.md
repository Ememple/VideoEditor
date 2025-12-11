<h1>Video Editor</h1>

  <p>
    App for editing videos
    <br>
    
  </p>
  

## Table of contents
- [Requirements](#requirements)
- [How to run](#how-to-run)
- [Functions](#functions)
- [What's included](#whats-included)
- [Contact](#contact)
- [Copyright and license](#copyright-and-license)

## Requirements
You need to have at least Java 17 installed

## How to run
1. Put the jar file and config in same folder
2. Open console
3. Change directory to where the jar file is located
4. Run the jar file

    ```
    java -jar <name.jar>
    ```

## Functions

### 1. Add Watermark (`ADD_WATERMARK`)

Embeds the watermark image onto all target video files

| Configuration Key | Description | Example Value |
| :--- | :--- | :--- |
| `operation` |Must be set to **`ADD_WATERMARK`** | `ADD_WATERMARK` |
| `input.path` | Absolute path to the folder containing videos | `C:/Videos/Source` |

### 2. Trim Video (`TRIM`)

Cuts the video file based on the specified start time and duration.

| Configuration Key | Description | Example Value |
| :--- | :--- | :--- |
| `operation` | Must be set to **`TRIM`** | `TRIM` |
| `input.path` | Absolute path to the folder containing videos | `C:/Videos/Source` |
| `trim.start` | Start time (Format: **HH:MM:SS**) | `00:00:10` |
| `trim.duration` | Duration (Format: **HH:MM:SS**) | `00:00:05` |

### 3. Convert Format (`CONVERT_FORMAT`)

Converts the video to a new specified format.

| Configuration Key | Description | Example Value |
| :--- | :--- | :--- |
| `operation` | Must be set to **`CONVERT_FORMAT`** | `CONVERT_FORMAT` |
| `input.path` | Absolute path to the folder containing videos | `C:/Videos/Source` |
| `format.target` | Target output format | `avi` (or `webm`, `mp4`) |

---

## What's included


```text
VideoEditor/
├── .idea/
├── src/
│   ├── java/
│   │   ├── ConsumerEncoder.java
│   │   ├── Main.java
│   │   ├── ProducerAnalyser.java 
│   │   ├── Reporter.java  
│   │   └── TaskData.java  
│   └── res/
│       ├── ffmpeg.exe
│       ├── ffprobe.exe
│       └── watermark.png
├── .gitignore
└── VideoEditor.iml
```
## Contact 
If you have any questions regarding this project contact me:<br>
 - E-mail: honzacihar@email.cz 

## Copyright and license

Code and documentation copyright 2025 the author. Code released under the [MIT License](https://github.com/Ememple/VideoEditor/blob/master/LICENSE).

