import React from 'react';
import {render, unmountComponentAtNode} from 'react-dom';
import VersionsMeasuresHistoryApp from './components/VersionsMeasuresHistoryApp';
import './style.css';

window.registerExtension('example/measures_history', options => {
    const {el} = options;
    render(
        <VersionsMeasuresHistoryApp
            project={options.component}
            options={options}
        />, el
    );
    return () => unmountComponentAtNode(el);
});
