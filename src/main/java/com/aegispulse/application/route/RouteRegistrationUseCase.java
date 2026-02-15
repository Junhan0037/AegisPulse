package com.aegispulse.application.route;

import com.aegispulse.application.route.command.RegisterRouteCommand;
import com.aegispulse.application.route.result.RegisterRouteResult;

/**
 * Route 등록 유스케이스 계약.
 */
public interface RouteRegistrationUseCase {

    RegisterRouteResult register(RegisterRouteCommand command);
}
