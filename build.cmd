java -jar ..\PluginWizardHelp\build\libs\plugin-wizard-help-1.20-SNAPSHOT.jar -rd "Jul 02, 2019" --out pages\help.xml --pluginFolder .

ectool login admin changeme

flowpdk build && ectool installPlugin build\EC-Bamboo.zip && ectool promotePlugin EC-Bamboo-1.5.0.0