import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
  signal,
} from '@angular/core';

interface BarcodeResultLike {
  rawValue: string;
}

interface BarcodeDetectorLike {
  detect(source: HTMLVideoElement): Promise<BarcodeResultLike[]>;
}

interface BarcodeDetectorConstructor {
  new (options?: { formats?: string[] }): BarcodeDetectorLike;
  getSupportedFormats?(): Promise<string[]>;
}

@Component({
  selector: 'app-barcode-scanner',
  templateUrl: './barcode-scanner.html',
  styleUrl: './barcode-scanner.scss',
})
export class BarcodeScannerComponent implements AfterViewInit, OnDestroy {
  @ViewChild('video', { static: true })
  private readonly videoRef!: ElementRef<HTMLVideoElement>;

  @Output() readonly scanned = new EventEmitter<string>();
  @Output() readonly closed = new EventEmitter<void>();

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  private stream: MediaStream | null = null;
  private detector: BarcodeDetectorLike | null = null;
  private animationFrame: number | null = null;
  private scanning = false;
  private finished = false;

  ngAfterViewInit(): void {
    void this.start();
  }

  ngOnDestroy(): void {
    this.stop();
  }

  close(): void {
    this.stop();
    this.closed.emit();
  }

  useManualCode(value: string): void {
    const code = value.trim();
    if (!code) {
      this.errorMessage.set('Ingresa un código de barras válido.');
      return;
    }
    this.finish(code);
  }

  private async start(): Promise<void> {
    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('La cámara no está disponible en este navegador.');
      }

      const detectorConstructor = (
        window as typeof window & { BarcodeDetector?: BarcodeDetectorConstructor }
      ).BarcodeDetector;

      if (!detectorConstructor) {
        throw new Error(
          'Este navegador no soporta lectura nativa de códigos de barras. Abre el sistema en Chrome desde el teléfono o ingresa el código manualmente.',
        );
      }

      const preferredFormats = [
        'ean_13',
        'ean_8',
        'upc_a',
        'upc_e',
        'code_128',
        'code_39',
        'itf',
        'codabar',
      ];

      const supported = detectorConstructor.getSupportedFormats
        ? await detectorConstructor.getSupportedFormats()
        : [];
      const formats = preferredFormats.filter((format) => supported.includes(format));

      this.detector = formats.length > 0
        ? new detectorConstructor({ formats })
        : new detectorConstructor();

      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
          facingMode: { ideal: 'environment' },
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      });

      const video = this.videoRef.nativeElement;
      video.srcObject = this.stream;
      await video.play();
      this.loading.set(false);
      this.scanFrame();
    } catch (error) {
      this.loading.set(false);
      this.errorMessage.set(
        error instanceof Error
          ? error.message
          : 'No fue posible iniciar la cámara.',
      );
    }
  }

  private scanFrame(): void {
    if (this.finished) {
      return;
    }

    this.animationFrame = requestAnimationFrame(() => {
      void this.detectFrame();
    });
  }

  private async detectFrame(): Promise<void> {
    const video = this.videoRef.nativeElement;

    if (!this.detector || this.scanning || video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) {
      this.scanFrame();
      return;
    }

    this.scanning = true;

    try {
      const results = await this.detector.detect(video);
      const value = results.find((result) => result.rawValue?.trim())?.rawValue.trim();

      if (value) {
        this.finish(value);
        return;
      }
    } catch {
      this.errorMessage.set('No se pudo leer el código. Mantén el código centrado y con buena iluminación.');
    } finally {
      this.scanning = false;
    }

    this.scanFrame();
  }

  private finish(code: string): void {
    if (this.finished) {
      return;
    }

    this.finished = true;
    this.stop();
    this.scanned.emit(code);
  }

  private stop(): void {
    if (this.animationFrame != null) {
      cancelAnimationFrame(this.animationFrame);
      this.animationFrame = null;
    }

    this.stream?.getTracks().forEach((track) => track.stop());
    this.stream = null;

    if (this.videoRef?.nativeElement) {
      this.videoRef.nativeElement.srcObject = null;
    }
  }
}