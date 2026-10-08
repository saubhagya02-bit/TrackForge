import { useEffect, useRef, useCallback } from "react";
import { Client, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

type Handler<T = unknown> = (data: T) => void;

export function useWebSocket() {
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map());

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("[WS] Connected");
      },
      onDisconnect: () => {
        console.log("[WS] Disconnected");
      },
      onStompError: (frame) => {
        console.error("[WS] Error:", frame);
      },
    });
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  const subscribe = useCallback(
    <T>(destination: string, handler: Handler<T>) => {
      const client = clientRef.current;
      if (!client?.connected) {
        const interval = setInterval(() => {
          if (clientRef.current?.connected) {
            clearInterval(interval);
            const sub = clientRef.current.subscribe(destination, (msg) => {
              handler(JSON.parse(msg.body) as T);
            });
            subscriptionsRef.current.set(destination, sub);
          }
        }, 200);
        return;
      }
      const sub = client.subscribe(destination, (msg) => {
        handler(JSON.parse(msg.body) as T);
      });
      subscriptionsRef.current.set(destination, sub);
    },
    [],
  );

  const unsubscribe = useCallback((destination: string) => {
    const sub = subscriptionsRef.current.get(destination);
    if (sub) {
      sub.unsubscribe();
      subscriptionsRef.current.delete(destination);
    }
  }, []);

  const publish = useCallback((destination: string, body: unknown) => {
    clientRef.current?.publish({
      destination,
      body: JSON.stringify(body),
    });
  }, []);

  return { subscribe, unsubscribe, publish };
}

export function useProjectBugUpdates(
  projectId: string | undefined,
  onUpdate: Handler,
) {
  const { subscribe, unsubscribe } = useWebSocket();

  useEffect(() => {
    if (!projectId) return;
    const dest = `/topic/projects/${projectId}/bugs`;
    subscribe(dest, onUpdate);
    return () => unsubscribe(dest);
  }, [projectId, subscribe, unsubscribe, onUpdate]);
}

export function useBugUpdates(bugId: string | undefined, onUpdate: Handler) {
  const { subscribe, unsubscribe } = useWebSocket();

  useEffect(() => {
    if (!bugId) return;
    const dest = `/topic/bugs/${bugId}`;
    subscribe(dest, onUpdate);
    return () => unsubscribe(dest);
  }, [bugId, subscribe, unsubscribe, onUpdate]);
}
