java -jar ..\PluginWizardHelp\build\libs\plugin-wizard-help-1.17-SNAPSHOT.jar -rd "Jun 13, 2019" --out pages\help.xml --pluginFolder . || exit 0

ectool login admin changeme

flowpdk build && ectool installPlugin build\EC-Bamboo.zip && ectool promotePlugin EC-Bamboo-1.5.0.0