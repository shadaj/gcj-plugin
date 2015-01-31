# gcj-plugin
Submit solutions to Google Code Jam contests via SBT

## Installation
Add the following to your plugins.sbt:

```scala
addSbtPlugin("me.shadaj" % "gcj-plugin" % "0.1-SNAPSHOT")
```

## Setup
Import gcj-plugin settings and set contestId, problemLaunchers, commonSources, and problemSources
```scala
import me.shadaj.gcj.Settings._

contestId := "2974486"

problemLaunchers := Map(
  "A" -> "me.shadaj.gcj.MagicTrickLauncher",
  "B" -> "me.shadaj.gcj.CookieClickerLauncher",
  "D" -> "me.shadaj.gcj.DeceitfulLauncher"
)

commonSources ++= Seq(
  baseDirectory.value / "build.sbt",
  baseDirectory.value / "project" / "plugins.sbt"
)

problemSources := Map(
  "A" -> Seq(baseDirectory.value / "src/main/scala/me/shadaj/gcj/MagicTrick.scala"),
  "B" -> Seq(baseDirectory.value / "src/main/scala/me/shadaj/gcj/CookieClicker.scala"),
  "D" -> Seq(baseDirectory.value / "src/main/scala/me/shadaj/gcj/DeceitfulWar.scala")
)
```

See the example qualification2014 project for a full build file

## Usage
### Check your status
```
> userStatus
```

### Download and submit a solution

```
> downloadRunAndSubmit A small
[info] Getting user status
[info] Points: 55
[info] Rank: 6504
[info] Problem: Magic Trick
[info] 	Set: small
[info] 		CORRECT! +6
[info] 		Attempts: 1
[info] 		Solved Time: 9.75 min
[info] 		Submitted: true
[info] Problem: Cookie Clicker Alpha
[info] 	Set: small
[info] 		CORRECT! +8
[info] 		Attempts: 1
[info] 		Solved Time: 36.92 min
[info] 		Submitted: true
...
```
