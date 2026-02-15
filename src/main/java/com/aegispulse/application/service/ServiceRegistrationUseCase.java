package com.aegispulse.application.service;

import com.aegispulse.application.service.command.RegisterServiceCommand;
import com.aegispulse.application.service.result.RegisterServiceResult;

/**
 * Service 등록 유스케이스 계약.
 */
public interface ServiceRegistrationUseCase {

    RegisterServiceResult register(RegisterServiceCommand command);
}
