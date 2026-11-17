import { ExtensionMessage } from '../types';

const PLATFORM_BASE_URL = 'http://localhost:3000';
const BACKEND_HEALTH_URL = 'http://localhost:8080/actuator/health';

if (typeof chrome !== 'undefined' && chrome.runtime) {
  chrome.runtime.onMessage.addListener((message: ExtensionMessage, _sender, sendResponse) => {
    if (message.type === 'OPEN_PLATFORM_TAB') {
      const payload = message.payload as { path?: string } | undefined;
      const targetUrl = payload?.path ? `${PLATFORM_BASE_URL}${payload.path}` : PLATFORM_BASE_URL;

      chrome.tabs.create({ url: targetUrl }, (tab) => {
        sendResponse({ success: true, tabId: tab.id });
      });
      return true;
    }

    if (message.type === 'CHECK_BACKEND_STATUS') {
      fetch(BACKEND_HEALTH_URL, { method: 'GET', mode: 'cors' })
        .then((res) => {
          if (res.ok) {
            sendResponse({
              online: true,
              platformUrl: PLATFORM_BASE_URL,
              authenticated: true,
            });
          } else {
            sendResponse({
              online: false,
              platformUrl: PLATFORM_BASE_URL,
              authenticated: false,
            });
          }
        })
        .catch(() => {
          sendResponse({
            online: false,
            platformUrl: PLATFORM_BASE_URL,
            authenticated: false,
          });
        });
      return true; // Asynchronous sendResponse
    }

    return false;
  });
}
