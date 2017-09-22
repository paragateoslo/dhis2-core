package org.hisp.dhis.sms.task;

/*
 * Copyright (c) 2004-2017, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.Parameters.SmsJobParameters;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.stream.Collectors;

public class SendSmsJob
    implements Job
{
    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Autowired
    private Notifier notifier;

    @Autowired
    private OutboundSmsService outboundSmsService;

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.SMS_SEND;
    }

    @Override
    public void execute( JobParameters jobParameters )
    {
        SmsJobParameters jobParams = (SmsJobParameters) jobParameters;

        notifier.notify( jobParams.getJobId(), "Sending SMS" );

        OutboundMessageResponse status = smsSender.sendMessage( jobParams.getSmsSubject(), jobParams.getText(), null, jobParams.getCurrentUser(), new HashSet<>( jobParams.getRecipientsList() ), false );

        OutboundSms sms = new OutboundSms();
        sms.setMessage( jobParams.getText() );
        sms.setRecipients( jobParams.getRecipientsList().stream().map( User::getPhoneNumber ).collect( Collectors.toSet() ) );

        if ( status.isOk() )
        {
            notifier.notify( jobParams.getJobId(), "Message sending successful" );

            sms.setStatus( OutboundSmsStatus.SENT );
        }
        else
        {
            notifier.notify( jobParams.getJobId(), "Message sending failed" );

            sms.setStatus( OutboundSmsStatus.FAILED );
        }

        outboundSmsService.saveOutboundSms( sms );
    }
}
