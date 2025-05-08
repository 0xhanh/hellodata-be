import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgModule,
  OnChanges,
  OnDestroy,
  OnInit,
  AfterViewInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { CommonModule } from "@angular/common";
import { HdCommonModule } from "../../../hd-common.module";
import { AuthService } from "../../services";
import { Subscription } from "rxjs";
import { environment } from "../../../../environments/environment";

/**
 * Component that embeds a subsystem in an iframe with authentication handling.
 * Supports both cookie-based auth and postMessage token passing.
 */
@Component({
  selector: 'app-subsystem-iframe[url]',
  templateUrl: './subsystem-iframe.component.html',
  styleUrls: ['./subsystem-iframe.component.scss']
})
export class SubsystemIframeComponent implements OnInit, OnDestroy, OnChanges, AfterViewInit {
  /** URL of the subsystem to embed */
  @Input() url!: string;
  /** Language code to pass to the subsystem */
  @Input() lang: string = '';
   /** Whether to include the access token in the URL query parameters */
  @Input() accessTokenInQueryParam = false;
  /** Delay in milliseconds before scrolling to top */
  @Input() delay = 0;
  /** Custom styles to apply to the iframe */
  @Input() style: { [p: string]: any } | null = null;
  /** Whether to post the token to the child iframe via postMessage */
  @Input() postTokenToChild: boolean = false;
   /** Event emitted when the iframe is set up */
  @Output() iframeSetup = new EventEmitter<boolean>();
  /** Reference to the iframe element */
  @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;
  /** The URL to display in the iframe */
  frameUrl!: string;
  /** Subscription to the auth service's access token */
  accessTokenSub!: Subscription;
  /** The current access token */
  private pendingToken: string | null = null;
  /** Whether the iframe has loaded */
  private iframeLoaded = false;
  /** Handler for postMessage events */
  private messageHandler: ((event: MessageEvent) => void) | null = null;
  /** Interval ID for sending token */
  private sendTokenInterval: number | null = null;
  /** Timeout ID for token sending operation */
  private timeoutId: number | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.setupTokenSubscription();
    if (this.postTokenToChild) {
      this.initializeMessageHandler();
    }
  }

  ngAfterViewInit() {
    console.debug('Iframe view initialized');
  }

  private setupTokenSubscription() {
    this.accessTokenSub = this.authService.accessToken.subscribe({
      next: (token) => {
        if (!token) return;

        console.debug('access token changed', token)
        // Store token for later use
        this.pendingToken = token;
        // Set cookie and update URL
        this.setAuthCookie(token);
        this.updateFrameUrl(token);
      },
      error: (error) => console.error('Token subscription error:', error)
    });
  }

  private setAuthCookie(token: string) {
    console.debug("creating an auth cookie for a domain: ." + environment.baseDomain);
    document.cookie = `auth.access_token=${token}; path=/; domain=.${environment.baseDomain}; secure;`;
  }

  private updateFrameUrl(token: string) {
    // Update URL immediately - no need for timeout
    this.frameUrl = this.accessTokenInQueryParam
      ? `${this.url}?auth.access_token=${token}`
      : this.url;
    this.iframeSetup.emit(true);

    // Use requestAnimationFrame for DOM operations
    requestAnimationFrame(() => {
      const mainContent = document.getElementById('mainContentDiv');
      if (mainContent) {
        mainContent.style.overflow = 'hidden';
      }

      // Only use setTimeout if absolutely necessary
      if (this.delay > 0) {
        setTimeout(() => this.scrollToTop(), this.delay);
      } else {
        this.scrollToTop();
      }
    });
  }

  private scrollToTop() {
    requestAnimationFrame(() => {
      const scrollButton = document.querySelector('.p-scrolltop-sticky') as HTMLElement;
      if (scrollButton) {
        scrollButton.click();
      }
    });
  }

  onIframeLoad() {
    this.iframeLoaded = true;
    console.debug('Iframe loaded: ', this.iframeLoaded);
  }

  private initializeMessageHandler(): void {
    if (!this.postTokenToChild) return;
  
    this.messageHandler = (event: MessageEvent) => {
      try {
        const targetUrl = new URL(this.url);
        if (event.origin !== targetUrl.origin) {
          console.warn('Message from untrusted origin:', event.origin);
          return;
        }
  
        switch(event.data.type) {
          case 'READY_FOR_TOKEN':
            console.log('Child iframe is ready for token');
            this.sendTokenWithRetry();
            break;
          case 'TOKEN_RECEIVED':
            console.log('Child iframe confirmed token receipt');

            this.cleanup();
            this.removeMessageHandler();
            break;
        }
      } catch (error) {
        console.error('Error handling message:', error);
      }
    };
  
    window.addEventListener('message', this.messageHandler);
  }

  /**
   * Removes the message event handler
   */
  private removeMessageHandler(): void {
    if (this.messageHandler) {
      window.removeEventListener('message', this.messageHandler);
      this.messageHandler = null;
    }
  }

  private sendToken(): void {
    if (!this.postTokenToChild || !this.iframe?.nativeElement?.contentWindow) {
      console.info('Token sending disabled or iframe not ready');
      return;
    }
  
    try {
      const targetUrl = new URL(this.url);
      const targetOrigin = targetUrl.origin;
      // Clear any existing intervals/timeouts
      this.cleanup();

      this.sendTokenInterval = setInterval(() => {
        // wait for token to be set
        if (!this.iframe?.nativeElement?.contentWindow || !this.pendingToken) {
          this.cleanup();
          return;
        }
        
        const messageData = {
          'access_token': this.pendingToken,
          'source': 'hellodata-portal',
          'lang': this.lang,
          'timestamp': new Date().getTime()
        };
  
        this.iframe.nativeElement.contentWindow.postMessage(
          messageData,
          targetOrigin
        );
        console.log('Token sent to:', targetOrigin);
      }, 500) as unknown as number;
  
      // Stop trying after 10 seconds
      this.timeoutId = setTimeout(() => {
        console.warn('Token sending operation timed out');
        this.cleanup();
      }, 10000) as unknown as number;
  
    } catch (error) {
      console.error('Error in sendToken:', error);
      throw error;
    }
  }

  /**
   * Sends the token with exponential backoff retry logic
   */
  private sendTokenWithRetry(maxRetries = 5, initialDelay = 300) {
    let attempts = 0;

    const trySend = () => {
      if (attempts >= maxRetries) {
        console.error('Max retries reached for sending token to iframe');
        return;
      }

      attempts++;
      try {
        this.sendToken();
      } catch (error) {
        // Calculate exponential backoff delay: initialDelay * 2^(attempts-1)
        // Example: 300ms, 600ms, 1200ms, etc.
        const backoffDelay = initialDelay * Math.pow(2, attempts - 1);

        // Add a small random jitter to prevent synchronized retries
        const jitter = Math.random() * 100;
        const finalDelay = backoffDelay + jitter;

        console.warn(`Retry attempt ${attempts} failed, retrying in ${Math.round(finalDelay)}ms`, error);
        setTimeout(trySend, finalDelay);
      }
    };

    trySend();
  }

  /**
   * Cleans up intervals and timeouts
   */
  private cleanup(): void {
    if (this.sendTokenInterval) {
      clearInterval(this.sendTokenInterval);
      this.sendTokenInterval = null;
    }
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }

  ngOnDestroy() {
    this.accessTokenSub?.unsubscribe();
    this.pendingToken = null;
    this.iframeLoaded = false;
    this.removeMessageHandler();
    this.cleanup()
    const mainContent = document.getElementById('mainContentDiv');
    if (mainContent) {
      mainContent.style.overflow = 'auto';
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if ('url' in changes) {
      this.accessTokenSub?.unsubscribe();
      this.setupTokenSubscription();
    }
  }
}

@NgModule({
  imports: [CommonModule, HdCommonModule],
  declarations: [SubsystemIframeComponent],
  exports: [SubsystemIframeComponent]
})
export class SubsystemIframeModule {}
