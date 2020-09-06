$from = $args[0]
$to = $args[1]
java -jar .\stitch-0.4.6+local-all.jar updateIntermediary ".\2fc0f18-$from.jar" ".\2fc0f18-$to.jar" ".\intermediary\$from.tiny" ".\intermediary\$to.tiny" ".\matches\$from-$to.match" -t tk/valoeghese/fc0/ -p ^[a-zA-Z$][a-z0-9$/]*[A-Z]?$
