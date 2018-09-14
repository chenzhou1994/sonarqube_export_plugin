package org.sonarsource.plugins.example.ws;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.*;

/**
 * 自定义导出插件的核心Action类，用户请求将在此类被处理
 *
 * @author chenzhou
 */
public class CustomExportAction implements CustomExportWsAction {
    private static final Logger LOGGER = Loggers.get(CustomExportAction.class);
    private static final String EXPORT_ACTION = "excel";

    public CustomExportAction() {
        LOGGER.info("Plugin == [CustomExportAction] --->> Construction_method  start executing...");
    }

    @Override
    public void define(WebService.NewController controller) {
        LOGGER.info("Plugin == [CustomExportAction] --->> define_method  start executing...");

        WebService.NewAction action = controller.createAction(EXPORT_ACTION)
                .setDescription("export issues.")
                .setSince("6.0")
                .setPost(true)
                .setHandler(this);
        action.createParam("issuesJsonData")
                .setDescription("export of the data 01")
                .setRequired(true);
        action.createParam("measuresJsonData")
                .setDescription("export of the data 02")
                .setRequired(true);
        action.createParam("imageJsonData")
                .setDescription("export of the data 03")
                .setRequired(true);
    }

    @Override
    public void handle(Request request, Response response) throws IOException {
        LOGGER.info("Plugin == [CustomExportAction] --->> handle start executing...");
        String issuesJsonData = request.mandatoryParam("issuesJsonData");
        String measuresJsonData = request.mandatoryParam("measuresJsonData");
        String imageJsonData = request.mandatoryParam("imageJsonData");
        JsonParseExport.exportExcel(measuresJsonData, issuesJsonData, imageJsonData, response);
    }
}

