import { Client, type IMessage } from '@stomp/stompjs';
import { useCallback, useRef } from 'react';
import type { LogStreamMessage } from '../types';

export function useLogStream(token: string | null) {
  const clientRef = useRef<Client | null>(null);

  const disconnect = useCallback(() => {
    if (clientRef.current?.active) {
      clientRef.current.deactivate();
    }
    clientRef.current = null;
  }, []);

  const startStream = useCallback(
    (
      serverId: number,
      commandKey: string,
      streamId: string,
      onMessage: (msg: LogStreamMessage) => void,
      onError: (err: string) => void
    ) => {
      disconnect();

      const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
      const wsUrl = `${protocol}://${window.location.host}/ws`;

      const client = new Client({
        brokerURL: wsUrl,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 0,
        onConnect: () => {
          client.subscribe('/topic/logs', (message: IMessage) => {
            const payload = JSON.parse(message.body) as LogStreamMessage;
            if (payload.streamId === streamId) {
              onMessage(payload);
            }
          });
          client.publish({
            destination: '/app/logs/stream/start',
            body: JSON.stringify({ serverId, commandKey, streamId }),
          });
        },
        onStompError: (frame) => {
          onError(frame.headers['message'] ?? 'WebSocket connection failed');
        },
        onWebSocketError: () => {
          onError('WebSocket connection error');
        },
      });

      clientRef.current = client;
      client.activate();
    },
    [token, disconnect]
  );

  const stopStream = useCallback(
    (streamId: string) => {
      if (clientRef.current?.connected) {
        clientRef.current.publish({
          destination: '/app/logs/stream/stop',
          body: JSON.stringify({ streamId }),
        });
      }
      disconnect();
    },
    [disconnect]
  );

  const startAppStream = useCallback(
    (
      logConfigId: number,
      streamId: string,
      onMessage: (msg: LogStreamMessage) => void,
      onError: (err: string) => void
    ) => {
      disconnect();

      const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
      const wsUrl = `${protocol}://${window.location.host}/ws`;

      const client = new Client({
        brokerURL: wsUrl,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 0,
        onConnect: () => {
          client.subscribe('/topic/logs', (message: IMessage) => {
            const payload = JSON.parse(message.body) as LogStreamMessage;
            if (payload.streamId === streamId) {
              onMessage(payload);
            }
          });
          client.publish({
            destination: '/app/logs/stream/app/start',
            body: JSON.stringify({ logConfigId, streamId }),
          });
        },
        onStompError: (frame) => {
          onError(frame.headers['message'] ?? 'WebSocket connection failed');
        },
        onWebSocketError: () => {
          onError('WebSocket connection error');
        },
      });

      clientRef.current = client;
      client.activate();
    },
    [token, disconnect]
  );

  return { startStream, startAppStream, stopStream, disconnect };
}
