/*
 * Copyright (C) 2017-2017 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
import React from 'react';
import MeasuresHistory from './MeasuresHistory'
import {translate} from '../common/l10n.js'
import {findVersionsAndMeasures} from '../api.js'
import {excelEduce} from '../api.js'


export default class VersionsMeasuresHistoryApp extends React.PureComponent {

    state = {
        data: []
    };

    componentDidMount() {
        const { project } = this.props;
        findVersionsAndMeasures(project).then(
            (valuesReturnedByAPI) => {
                this.setState({
                    data: valuesReturnedByAPI
                });
            }
        );
    }

    // 导出
    clickExportExcel() {
        const { project, options } = this.props
        excelEduce(project, options);
    }

    render() {
        // Data Gathered: {JSON.stringify(this.state.data)}
        const { data } = this.state;
        return (
            <div className="page page-limited">
                <table className="data zebra">
                    <thead>
                    <tr className="code-components-header">
                        <th className="thin nowrap text-left code-components-cell">版本</th>
                        <th className="thin nowrap text-center code-components-cell">质量等级</th>
                        <th className="thin nowrap text-right code-components-cell">{translate('issue.type.BUG.plural')}</th>
                        <th className="thin nowrap text-right code-components-cell">可靠性等级</th>
                        <th className="thin nowrap text-right code-components-cell">漏洞</th>
                        <th className="thin nowrap text-right code-components-cell">安全等级</th>
                        <th className="thin nowrap text-right code-components-cell">代码异味</th>
                        <th className="thin nowrap text-right code-components-cell">可维护性等级</th>
                    </tr>
                    </thead>
                    <tbody>
                    { data.map((value, idx) =>
                        <MeasuresHistory measure={value} key={idx}/>
                        )}
                    </tbody>
                </table>
                <br/>
                <br/>
                <div>
                    <h3>以上为进行JenFire项目分析得出的简要信息。</h3>
                    <span> 点击下方导出按钮可下载详细分析结果，您可对导出结果进行编辑。</span>
                    <br/><br/>
                    <button onClick={() => this.clickExportExcel()}>导出详细结果 ...</button>
                </div>
            </div>
        );
    }
}
