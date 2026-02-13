import React, { useState, useEffect, useRef, useCallback } from 'react';
import { toast } from 'react-toastify';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { messagesApi } from '../utils/api';

function UserCaseMessages({ caseId, userId, userType, lawyerId, clientUserId, caseStatus, onCaseUpdate }) {
  const isPending = caseStatus?.toUpperCase() === 'PENDING_APPROVAL';
  const isClosed = caseStatus?.toUpperCase() === 'CLOSED';
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);

  const fetchMessages = useCallback(async () => {
    if (!caseId) return;
    try {
      const response = await messagesApi.getByCase(caseId);
      setMessages(Array.isArray(response.data) ? response.data : []);
    } catch (err) { console.error('Error fetching messages:', err); }
  }, [caseId]);

  const connectWebSocket = useCallback(() => {
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const socketUrl = `${protocol}//${window.location.hostname}:8080/ws`;
    const socket = new SockJS(socketUrl);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/case/${caseId}`, (message) => {
          const receivedData = JSON.parse(message.body);
          if (receivedData.messageText) {
            setMessages((prev) => prev.some(m => m.id === receivedData.id) ? prev : [...prev, receivedData]);
          } else if (onCaseUpdate) onCaseUpdate(receivedData);
        });
      },
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });
    client.activate();
    stompClientRef.current = client;
  }, [caseId, onCaseUpdate]);

  useEffect(() => {
    if (caseId) { fetchMessages(); connectWebSocket(); }
    return () => stompClientRef.current?.deactivate();
  }, [caseId, fetchMessages, connectWebSocket]);

  useEffect(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  const sendMessage = async () => {
    if (!newMessage.trim() || isPending) return;
    const isLawyer = userType === 'lawyer';
    const receiverId = isLawyer ? clientUserId : lawyerId;
    if (!receiverId) return toast.error('Waiting for other party...');

    const messageData = {
      caseId, senderId: userId, senderType: userType,
      receiverId, receiverType: isLawyer ? 'user' : 'lawyer',
      messageText: newMessage.trim()
    };

    if (stompClientRef.current && connected) {
      stompClientRef.current.publish({ destination: '/app/chat.send', body: JSON.stringify(messageData) });
      setNewMessage('');
    } else {
      try { await messagesApi.send(messageData); setNewMessage(''); }
      catch (err) { toast.error('Failed to send'); }
    }
  };

  return (
    <div className="flex flex-col h-[600px] bg-white dark:bg-background-dark/30 rounded-3xl border border-gray-100 dark:border-gray-800 overflow-hidden shadow-sm">
      {/* Chat Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex justify-between items-center bg-gray-50/50 dark:bg-white/5 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-electric-blue/10 rounded-lg flex items-center justify-center">
            <span className="material-symbols-outlined text-electric-blue !text-lg">forum</span>
          </div>
          <h3 className="text-sm font-black uppercase tracking-widest text-primary dark:text-white">Secure Link</h3>
        </div>
        <div className={`flex items-center gap-2 px-3 py-1 rounded-full border ${connected ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-500' : 'bg-red-500/10 border-red-500/20 text-red-500'}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${connected ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'}`}></span>
          <span className="text-[10px] font-black uppercase tracking-tighter">{connected ? 'Live' : 'Offline'}</span>
        </div>
      </div>

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-hide">
        {messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center opacity-40 grayscale">
            <span className="material-symbols-outlined text-4xl mb-2">mark_chat_unread</span>
            <p className="text-[10px] font-black uppercase tracking-[0.2em]">Establish communication below</p>
          </div>
        ) : (
          messages.map((msg) => {
            const isMe = String(msg.senderId) === String(userId) && msg.senderType === userType;
            return (
              <div key={msg.id || Math.random()} className={`flex ${isMe ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2`}>
                <div className={`max-w-[80%] ${isMe ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
                  <span className="text-[10px] font-black uppercase tracking-widest text-gray-400 px-1">
                    {isMe ? 'Internal' : msg.senderType === 'lawyer' ? 'Counsel' : 'Client'}
                  </span>
                  <div className={`p-4 rounded-2xl text-sm font-medium leading-relaxed shadow-sm border ${isMe
                    ? 'bg-primary text-white border-primary-dark rounded-tr-none'
                    : 'bg-white dark:bg-gray-800 text-primary dark:text-gray-200 border-gray-100 dark:border-gray-700 rounded-tl-none'
                    }`}>
                    {msg.messageText}
                  </div>
                  <span className="text-[8px] font-bold text-gray-400 uppercase tracking-tighter">
                    {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Transmitting...'}
                  </span>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 bg-gray-50/50 dark:bg-white/5 border-t border-gray-100 dark:border-gray-800">
        <div className="relative group">
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
            placeholder={isClosed ? "Case Archived: Locked" : isPending ? "Authorization Required..." : "Inject transmission..."}
            disabled={isPending || isClosed || (!lawyerId && userType === 'user')}
            className="w-full pl-6 pr-14 py-4 bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 text-sm focus:ring-2 focus:ring-primary outline-none transition-all disabled:opacity-50"
          />
          <button
            onClick={sendMessage}
            disabled={isPending || isClosed || !newMessage.trim()}
            className="absolute right-2 top-2 w-10 h-10 bg-primary text-white rounded-xl flex items-center justify-center hover:scale-105 active:scale-95 transition-transform disabled:grayscale shadow-lg shadow-primary/20"
          >
            <span className="material-symbols-outlined !text-lg">send</span>
          </button>
        </div>
        {!lawyerId && userType === 'user' && !isClosed && (
          <div className="mt-3 text-center">
            <span className="text-[9px] font-black uppercase text-amber-500 tracking-widest animate-pulse">Awaiting Expert Assignment</span>
          </div>
        )}
        {isClosed && (
          <div className="mt-3 text-center">
            <span className="text-[9px] font-black uppercase text-gray-500 tracking-widest flex items-center justify-center gap-2">
              <span className="material-symbols-outlined !text-xs">lock</span>
              Professional record finalized. Communication locked.
            </span>
          </div>
        )}
      </div>
    </div>
  );
}

export default UserCaseMessages;
