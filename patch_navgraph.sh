#!/bin/bash
sed -i '/composable<Screen.EbServiceNumber> {/,/}/!b;//!d;/}/!d' /app/applet/app/src/main/java/com/example/navigation/NavGraph.kt
