# Copyright (C) 2010 Ion Torrent Systems, Inc. All Rights Reserved
# $Revision: 43001 $
VERSION="3.0.5"

# BAM and SFF File Name
RUN_NAME=${TSP_RUN_NAME}
ANALYSIS_NAME=${TSP_ANALYSIS_NAME}
BAM_FILE=${RUN_NAME}_${TSP_ANALYSIS_NAME}.bam
SFF_FILE=${RUN_NAME}_${TSP_ANALYSIS_NAME}.sff
DIRNAME=${RUNINFO__PLUGIN_DIR}
PLUGINOUT=${RUNINFO__RESULTS_DIR}
URL_ROOT=${RUNINFO__NET_LOCATION}
PLUGIN_DIR=${TSP_FILEPATH_PLUGIN_DIR}

# set +e
set +u nounset
echo URL_ROOT=${URL_ROOT}
echo RUNINFO_URL_ROOT=${RUNINFO__URL_ROOT}
echo RAW_DATA_DIR=${RAW_DATA_DIR}
echo ANALYSIS_DIR=${ANALYSIS_DIR}
echo RESULTS_DIR=${RESULTS_DIR}
echo TSP_CHIP_TYPE=${TSP_CHIPTYPE}
echo DIRNAME=${DIRNAME}
echo PLUGINOUT=${PLUGINOUT}
echo SFF=${SFF_FILE}
echo BAM=${BAM_FILE}
echo

echo
qstat -f | grep plugin.q > ${RESULTS_DIR}/nodes.txt

echo java -Xms4G -Xmx8G -jar ${DIRNAME}/Preparer.jar -cache ${ANALYSIS_DIR} -res ${ANALYSIS_DIR} -raw ${RAW_DATA_DIR} -plugin ${RESULTS_DIR}  -sff ${SFF_FILE} -bam ${BAM_FILE}
java -Djava.awt.headless=true -Xms4G -Xmx8G -jar ${DIRNAME}/Preparer.jar -cache ${ANALYSIS_DIR} -res ${ANALYSIS_DIR} -raw ${RAW_DATA_DIR} -plugin ${RESULTS_DIR} -sff ${SFF_FILE} -bam ${BAM_FILE} -key ${RUNINFO__LIBRARY_KEY}
if [ "$?" -ne 0 ]; then
    echo "bad exit status from Preparer.jar, will still create link"
fi


