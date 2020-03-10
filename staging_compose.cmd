::staging_compose.bat

@echo off

@rem read envs
for /f "delims== tokens=1,2" %%G in (.env) do set %%G=%%H

set SERVICE_NAME=%1

if /I not "%SERVICE_NAME%"=="streaming-app" (
   if /I not "%SERVICE_NAME%"=="news-collector" (
      if /I not "%SERVICE_NAME%"=="tesla-stocks-collector" (
          if /I not "%SERVICE_NAME%"=="musk-tweets-collector" (
             echo "should supply name of the service [streaming-app|news-collector|tesla-stocks-collector|musk-tweets-collector]"
             exit /B 1
          )
      )
   )
)

@rem get all but firsts arguments
echo all args: %*
for /f "tokens=1,* delims= " %%a in ("%*") do set ALL_BUT_FIRST=%%b
echo all but first: %ALL_BUT_FIRST%

@rem construct compose command
ecs-cli compose^
   --cluster-config ucu-class^
   --region us-east-1^
   --debug^
   --file staging-musk-tweets-collector.yml^
   --project-name %STUDENT_NAME%-musk-tweets-collector^
   service %ALL_BUT_FIRST%

@rem construct compose command
ecs-cli compose^
   --cluster-config ucu-class^
   --region us-east-1^
   --debug^
   --file staging-news-collector.yml^
   --project-name %STUDENT_NAME%-news-collector^
   service %ALL_BUT_FIRST%

@rem construct compose command
ecs-cli compose^
   --cluster-config ucu-class^
   --region us-east-1^
   --debug^
   --file staging-tesla-stocks-collector.yml^
   --project-name %STUDENT_NAME%-tesla-stocks-collector^
   service %ALL_BUT_FIRST%

@rem construct compose command
ecs-cli compose^
   --cluster-config ucu-class^
   --region us-east-1^
   --debug^
   --file staging-streaming-app.yml^
   --project-name %STUDENT_NAME%-streaming-app^
   service %ALL_BUT_FIRST%
