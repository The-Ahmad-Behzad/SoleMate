import { bootstrapCameraKit, CameraKitSession, CameraKit, Lens } from '@snap/camera-kit';

export class SnapARService {
  private static instance: SnapARService;
  private session: CameraKitSession | null = null;
  private cameraKit: CameraKit | null = null;
  private currentCanvas: HTMLCanvasElement | null = null;

  // TODO: INSERT YOUR SNAP API TOKEN HERE
  private readonly apiToken = 'eyJhbGciOiJIUzI1NiIsImtpZCI6IkNhbnZhc1MyU0hNQUNQcm9kIiwidHlwIjoiSldUIn0.eyJhdWQiOiJjYW52YXMtY2FudmFzYXBpIiwiaXNzIjoiY2FudmFzLXMyc3Rva2VuIiwibmJmIjoxNzY5MTY2OTEzLCJzdWIiOiJhMjBjNWYyYS0yY2FkLTQxNDAtOTVlZC1jZGNlNGY1YWQwYjF-U1RBR0lOR343ZGExMGU0Ni0zNzRhLTQxZDktOWFmYS00OWFmNDc1YmYzOWMifQ.hOalDSA0aYTH_dbBMdCBMuUeOr4ukyx_q63WhJf_KF8';
  private readonly lensGroupId = 'aee0a448-e956-4af3-9826-ea32e5a2092e';
  private readonly defaultLensId = 'ea4344fa-399b-45ea-814b-ea4b165a6afc';

  private constructor() { }

  static getInstance(): SnapARService {
    if (!SnapARService.instance) {
      SnapARService.instance = new SnapARService();
    }
    return SnapARService.instance;
  }

  async initialize(canvas: HTMLCanvasElement): Promise<void> {
    // If session exists and canvas is the same, do nothing
    if (this.session && this.currentCanvas === canvas) {
      console.log('SnapAR: Reusing existing session for same canvas');
      return;
    }

    // If canvas is different, we must clean up the old session
    if (this.session && this.currentCanvas !== canvas) {
      console.log('SnapAR: Canvas changed, cleaning up old session');
      await this.destroy();
    }

    try {
      console.log('SnapAR: Initializing SDK...');
      if (!this.cameraKit) {
        this.cameraKit = await bootstrapCameraKit({
          apiToken: this.apiToken,
        });
      }

      console.log('SnapAR: Creating session for canvas...');
      this.session = await this.cameraKit.createSession({
        liveRenderTarget: canvas,
      });
      this.currentCanvas = canvas;

      console.log('SnapAR: Snap Camera Kit initialized');
    } catch (error) {
      console.error('SnapAR: Failed to initialize Snap Camera Kit:', error);
      throw error;
    }
  }

  async startCamera(): Promise<void> {
    if (!this.session) throw new Error('Session not initialized');

    try {
      console.log('SnapAR: Requesting camera permissions...');
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
      });

      console.log('SnapAR: Setting camera source...');
      await this.session.setSource(mediaStream);
      await this.session.play();
      console.log('SnapAR: Camera started');
    } catch (error) {
      console.error('SnapAR: Failed to start camera:', error);
      throw error;
    }
  }

  async applyLens(lensId: string = this.defaultLensId): Promise<void> {
    if (!this.session || !this.cameraKit) throw new Error('Session not initialized');

    try {
      console.log(`SnapAR: Loading lens ${lensId}...`);
      const lens = await this.cameraKit.lensRepository.loadLens(
        lensId,
        this.lensGroupId
      );
      
      console.log('SnapAR: Applying lens to session...');
      await this.session.applyLens(lens);
      console.log(`SnapAR: Applied lens: ${lensId}`);
    } catch (error) {
      console.error('SnapAR: Failed to apply lens:', error);
      throw error;
    }
  }

  async stop(): Promise<void> {
    if (this.session) {
      console.log('SnapAR: Pausing session...');
      await this.session.pause();
    }
  }

  async destroy(): Promise<void> {
    if (this.session) {
      console.log('SnapAR: Destroying session...');
      await this.session.pause();
      this.session.destroy();
      this.session = null;
      this.currentCanvas = null;
    }
  }

  async takeSnapshot(): Promise<Blob> {
    if (!this.session) throw new Error('Session not initialized');

    console.log('SnapAR: Capturing snapshot...');
    // Camera Kit session.output yields the current frame
    const canvas = this.session.output.live;
    return new Promise((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (blob) {
          console.log('SnapAR: Snapshot captured');
          resolve(blob);
        } else {
          reject(new Error('Failed to create blob from canvas'));
        }
      }, 'image/png');
    });
  }
}

export const snapAR = SnapARService.getInstance();
