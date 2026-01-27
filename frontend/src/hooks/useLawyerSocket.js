import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const useLawyerSocket = ({
    lawyerId,
    lawyerProfileRef,
    onNewRequest,
    onCaseAssigned,
    onCaseDeleted
}) => {
    const clientRef = useRef(null);

    useEffect(() => {
        if (!lawyerId) {
            console.log('WS SKIP: No lawyerId available for connection');
            return;
        }

        console.log('WS INIT: Starting connection for lawyer:', lawyerId);
        const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
        const host = window.location.hostname;
        const port = '8080';
        const socketUrl = `${protocol}//${host}:${port}/ws`;

        const socket = new SockJS(socketUrl);
        const client = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('WS CONNECT: Connected to request service');

                // Subscribe to new case requests
                client.subscribe('/topic/lawyer/requests', (message) => {
                    if (!onNewRequest) return;

                    const receivedPayload = JSON.parse(message.body);
                    const currentProfile = lawyerProfileRef.current;

                    console.log('WS RECEIVED: Raw payload:', receivedPayload);

                    // Filter by specialization
                    const specsPlural = (currentProfile?.specializations || "").toLowerCase();
                    const specsSingular = (currentProfile?.specialization || "").toLowerCase();
                    const category = (receivedPayload.category || receivedPayload.caseCategory || "").toLowerCase();

                    const isMatch = !currentProfile ||
                        specsPlural.includes(category) ||
                        specsSingular.includes(category) ||
                        category === "" ||
                        (specsPlural === "" && specsSingular === "");

                    if (isMatch) {
                        onNewRequest(receivedPayload);
                    } else {
                        console.log(`WS FILTER SKIP: Request for "${category}" does not match lawyer specializations.`);
                    }
                });

                // Subscribe to global lawyer updates
                client.subscribe('/topic/lawyer/updates', (message) => {
                    const update = JSON.parse(message.body);
                    console.log('WS UPDATE RECEIVED:', update);

                    if (update.type === 'CASE_ASSIGNED') {
                        if (onCaseAssigned) onCaseAssigned(update.caseId);
                    } else if (update.type === 'CASE_DELETED') {
                        if (onCaseDeleted) onCaseDeleted(update.caseId);
                    }
                });
            },
            onStompError: (frame) => {
                console.error('WS ERROR:', frame.headers['message']);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            console.log('WS CLEANUP: Deactivating client');
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [lawyerId, lawyerProfileRef, onNewRequest, onCaseAssigned, onCaseDeleted]); // Dependencies

    return clientRef.current;
};

export default useLawyerSocket;
