memory=1496m
workspace_path=ws_$1
output_folder=output_$1
now=$(date +"%F")
logfile=log_$1_$now.log
#errfile=err_$1_$now.log
cmdline="-data $workspace_path -q $2 -n $3 -o $output_folder"
echo -------------------------------
echo $cmdline
eclipse/eclipse $cmdline &> $logfile
./dotall.sh $output_folder/layer_2/dot/
