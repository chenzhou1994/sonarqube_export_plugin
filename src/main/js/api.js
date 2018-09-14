/*
 * Copyright (C) 2017-2017 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
import {getJSON} from 'sonar-request'; // see https://github.com/SonarSource/sonarqube/blob/master/server/sonar-web/src/main/js/app/utils/exposeLibraries.js
import {request} from 'sonar-request';

export function findQualityProfilesStatistics(project) {
    return getJSON('/api/qualityprofiles/search').then(function (response) {
        return response.profiles.length;
    });
};

export function findQualityQatesStatistics(project) {
    return getJSON('/api/qualitygates/list').then(function (response) {
        return response.qualitygates.length;
    });
};

export function findIssuesStatistics(project) {
    return getJSON('/api/issues/search').then(function (response) {
        return response.total;
    });
};

export function findProjects(project) {
    return getJSON('/api/projects/search').then(function (response) {
        return response.components.length;
    });
};

export function findVersionsAndMeasures(project) {

    return getJSON('/api/project_analyses/search', {
        project: project.key,
        p: 1,
        ps: 500,
    }).then(function (responseAnalyses) {
        const numberOfAnalyses = responseAnalyses.analyses.length;
        if (numberOfAnalyses > 0) {

            return getJSON('/api/measures/search_history', {
                component: project.key,
                metrics: "alert_status,bugs,vulnerabilities,code_smells,reliability_rating,security_rating,sqale_rating",
                ps: 1000
            }).then(function (responseMetrics) {
                var data = [];
                var numberOfVersions = 0;

                for (let i = 0; i < numberOfAnalyses; i++) {
                    let analysis = responseAnalyses.analyses[i];
                    for (let j = 0; j < analysis.events.length; j++) {
                        if (analysis.events[j].category === "VERSION") {
                            let result = {
                                version: analysis.events[j].name,
                                alert_status: "",
                                bugs: "0", vulnerabilities: "0", code_smells: "0",
                                reliability_rating: "", security_rating: "", sqale_rating: ""
                            };
                            const numberOfMeasuresRetrieved = 7;

                            for (let k = 0; k < numberOfMeasuresRetrieved; k++) {
                                for (let d = 0; d < responseMetrics.measures[k].history.length; d++) {
                                    if (responseMetrics.measures[k].history[d].date === responseAnalyses.analyses[i].date) {
                                        //console.log(responseMetrics.measures[k].metric);
                                        if (responseMetrics.measures[k].metric === "bugs") {
                                            result.bugs = responseMetrics.measures[k].history[d].value;
                                        } else if (responseMetrics.measures[k].metric === "vulnerabilities") {
                                            result.vulnerabilities = responseMetrics.measures[k].history[d].value;
                                        } else if (responseMetrics.measures[k].metric === "code_smells") {
                                            result.code_smells = responseMetrics.measures[k].history[d].value;
                                        } else if (responseMetrics.measures[k].metric === "alert_status") {
                                            result.alert_status = responseMetrics.measures[k].history[d].value;
                                        } else if (responseMetrics.measures[k].metric === "reliability_rating") {
                                            result.reliability_rating = responseMetrics.measures[k].history[d].value;
                                        } else if (responseMetrics.measures[k].metric === "security_rating") {
                                            result.security_rating = responseMetrics.measures[k].history[d].value;
                                        } else if (responseMetrics.measures[k].metric === "sqale_rating") {
                                            result.sqale_rating = responseMetrics.measures[k].history[d].value;
                                        }
                                    }
                                }
                            }

                            data[numberOfVersions] = result;
                            numberOfVersions++;
                        }
                    }
                }
                //console.table(data);
                return data;
            });
        }
    });
}


export function ExcelEduce(project, options) {
    var issuesResponse;
    var imageResponse;
    var measuresResponse;
    getJSON('/api/issues/search', {
        componentKeys: project.key,
        s: "FILE_LINE",
        resolved: false,
        ps: 100,
        facets: "severities,types",
        additionalFields: "_all"
    }).then(function (response) {
        issuesResponse = response;
        return runAsync2(project);
    }).then(function (data) {
        measuresResponse = data;
        return runAsync3(project);
    }).then(function (value) {
        imageResponse = value;
        var jd_01 = JSON.stringify(issuesResponse);
        var jd_02 = JSON.stringify(measuresResponse);
        var jd_03 = JSON.stringify(imageResponse);
        window.SonarRequest.request('/api/custom_export/excel')
            .setMethod('POST')
            .setData({issuesJsonData: jd_01, measuresJsonData: jd_02, imageJsonData: jd_03})
            .submit().then(function (value) {
            return value.blob();
        }).then(function (blob) {
            var reader = new FileReader();
            // 转换为base64，可以直接放入href
            reader.readAsDataURL(blob);
            reader.onload = function (e) {
                // 转换完成，创建一个a标签用于下载
                var a = document.createElement('a');
                a.download = 'Report.xlsx';
                a.href = e.target.result;
                options.el.appendChild(a);
                a.click();
                a.remove();
            }
        });
    });
}

export function runAsync2(project) {
    //alert("runAsync2");
    var p = new Promise(function (resolve, reject) {
        getJSON('/api/measures/component', {
            additionalFields: "metrics,periods",
            componentKey: project.key,
            metricKeys: "alert_status,bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density"
        }).then(function (data) {
            //alert("data"+data.paging.total);
            resolve(data);
        });
    });
    return p;
}

export function runAsync3(project) {
    //alert("runAsync3");
    var p = new Promise(function (resolve, reject) {
        getJSON('/api/measures/search_history', {
            component: project.key,
            metrics: "bugs,code_smells,vulnerabilities",
            ps: 1000
        }).then(function (data) {
            resolve(data);
        });
    });
    return p;
}



