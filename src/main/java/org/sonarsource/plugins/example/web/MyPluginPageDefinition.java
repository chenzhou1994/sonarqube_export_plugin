package org.sonarsource.plugins.example.web;

import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Scope;
import org.sonar.api.web.page.PageDefinition;

/**
 * 自定义页面选项卡插件
 *
 * @author chenzhou
 */
public class MyPluginPageDefinition implements PageDefinition {

    @Override
    public void define(Context context) {
        context.addPage(Page.builder("example/measures_history")
                        .setName("项目导出")
                        .setScope(Scope.COMPONENT).build());
    }
}
