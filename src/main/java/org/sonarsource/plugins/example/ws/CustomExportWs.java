package org.sonarsource.plugins.example.ws;

import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;


import java.util.List;

/**
 * 自定义导出插件的核心Controller类,根据url匹配此Controller
 *
 * @author chenzhou
 */
public class CustomExportWs implements WebService {
    private static final Logger LOGGER = Loggers.get(CustomExportWs.class);
    public static final String CUSTOM_EXPORT_CONTROLLER = "api/custom_export";
    private final List<CustomExportWsAction> actions;
    public CustomExportWs(List<CustomExportWsAction> actions) {
        this.actions = actions;
    }


    @Override
    public void define(Context context) {
        LOGGER.info("Plugin == [CustomExportWs] --->> define_method  start executing...");
        NewController controller = context.createController(CUSTOM_EXPORT_CONTROLLER);
        controller.setDescription("Handle custom_export.");
        actions.forEach(action -> action.define(controller));
        controller.done();
    }
}
