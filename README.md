# JADE
JADE stands for Joint Advanced Defect assEsment for android applications written in 2014. JADE is a tool written in Java and Scala with the power of Soot to provide 
both interprocedure and intraprocedure static analysis for android applications. Its features include API misuse analysis, local-denial-of-service
(intent crash) analysis, inter-procedure style taint flow analysis (from intent to sensitive API, i.e. getting a parcelable from intent, and
use it to start activity).

JADE can also combines multidex into one and analysis them altogether. Most of JADE's detection capabilities can be defined in groovy config file and text file (soot's source and sink file).

# USAGE
JADE is packed into a single jar archive and I provide a default vulnerability rules file. There're two major mode for JADE.

## FullAnalysis
`FullAnalysis` unleash the full power of JADE and Soot, including inter-procedure whole-application analysis and inter-procedure dataflow analysis.
But it may also consume much time and may not finish on machines with small memory (<16GB). Default is full-mode.
## FastAnalysis
`FastAnalysis` usually finishes in less than 1 minute and is intended for large-scale batch analysis. Inter-procedure analysis is disabled
to achieve maxmium flexibility. In normal situations this mode is enough for common audit.

--fastanalysis enables fastanalysis and disables fullanalysis.

Command line for analysis:
`java -jar jade-0.1.jar vulnanalysis -f 1.apk -p /Users/hqdvista/android-sdks/platforms/ -c /Users/hqdvista/Dropbox/keen/Jade-devs/jade/config/ --fastanalysis`

###-c option
-c must be provided as the directory for config files, including taint rules, source and sink, vulnerable API description and so on. If you do
not understand the config files content, do not modify them, leave them as it is.

###-p option
-p option specifies the android platform directory, which usually just points to ${ANDROID_SDK}/platforms/

### -f option
-f option specifies the APK to be analyzed.

## Output
JADE will output result to in a list to console and also writes json-ed result to output/ directory: {MD5_OF_INPUT_APK}.txt. A sample can be 
find in output directory of this repo: https://github.com/flankerhqd/JADE/blob/master/output/92db77bbe1cae9004f11ef9d3d6cbf08.txt

Snippet:

```json
  }, {
    "desc": "sensitive data flow",
    "sourceStmt": "$r24 = virtualinvoke $r2.<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>($r24)",
    "custom": "",
    "vulnKind": 2,
    "destMethod": "<cn.jpush.android.service.PushReceiver: void onReceive(android.content.Context,android.content.Intent)>",
    "paths": [],
    "destStmt": "virtualinvoke $r1.<android.content.Context: void sendBroadcast(android.content.Intent,java.lang.String)>($r27, $r24)",
    "sourceMethod": "<cn.jpush.android.service.PushReceiver: void onReceive(android.content.Context,android.content.Intent)>"
  }, {
    "desc": "sensitive data flow",
    "sourceStmt": "$r4 = virtualinvoke $r2.<android.content.Intent: android.os.Bundle getExtras()>()",
    "custom": "",
    "vulnKind": 2,
    "destMethod": "<com.fugao.fxhealth.receiver.JPushReceiver: void onReceive(android.content.Context,android.content.Intent)>",
    "paths": [],
    "destStmt": "virtualinvoke $r1.<android.content.Context: void startActivity(android.content.Intent)>($r2)",
    "sourceMethod": "<com.fugao.fxhealth.receiver.JPushReceiver: void onReceive(android.content.Context,android.content.Intent)>"
  }, {
    "desc": "sensitive data flow",
    "sourceStmt": "$r6 = virtualinvoke $r2.<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>($r6)",
    "custom": "",
    "vulnKind": 2,
    "destMethod": "<cn.jpush.android.data.x: void a(android.content.Context)>",
    "paths": [],
    "destStmt": "virtualinvoke $r1.<android.content.Context: void startActivity(android.content.Intent)>($r2)",
    "sourceMethod": "<cn.jpush.android.service.PushReceiver: void onReceive(android.content.Context,android.content.Intent)>"
  }, {
    "desc": "sensitive data flow",
    "sourceStmt": "$r9 = virtualinvoke $r2.<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>($r9)",
    "custom": "",
    "vulnKind": 2,
    "destMethod": "<cn.jpush.android.data.x: void a(android.content.Context)>",
    "paths": [],
    "destStmt": "virtualinvoke $r1.<android.content.Context: void startActivity(android.content.Intent)>($r2)",
    "sourceMethod": "<cn.jpush.android.service.PushReceiver: void onReceive(android.content.Context,android.content.Intent)>"
  }]}
```

# Hint
To avoid OOM, add -Xmx option to commandline, e.g. java -jar jade-0.1.jar -Xmx8192m

# Build from source code

JADE is organized by gradle. Run `gradle fatJar` at the root of source directory and single-bundled jar will be generated at ./jade/build/
Each directory actually represents a git submodule originally. For simplicity I am combining them to same root directories and you can change it 
as you wish, track upstream Soot changes.

# Technical Description

https://speakerdeck.com/flankerhqd/jade-joint-advanced-defect-assesment

# Credits
Thanks Soot authors (https://github.com/Sable/soot) for providing such a good framework.

# Disclaimer:
This is just a research prototype, use at your own risk. The results may contain false positives and false negatives due to the nature of static
analysis. Feel free to fork and pull it.
