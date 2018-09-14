package org.sonarsource.plugins.example.ws;

import org.sonar.api.server.ws.Definable;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.WebService;

/**
 * @author chenzhou
 */
public interface WsAction extends RequestHandler, Definable<WebService.NewController> {
}
