# gcj-plugin
Submit solutions to Google Code Jam contests via SBT

## WARNING: USE AT YOUR OWN RISK!
Please raise any issues you find through GitHub issues.

## Installation
Add the following to your plugins.sbt:

```scala
addSbtPlugin("me.shadaj" % "gcj-plugin" % "0.1-SNAPSHOT")
```

## Setup
Enable the plugin and set contestId, problemLaunchers, commonSources, and problemSources
```scala
enablePlugins(GCJPlugin)

contestId := "2974486" // found in the contest page url

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
> gcjStatus
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

### Download and submit a solution

```
> gcjDrs A small
```
