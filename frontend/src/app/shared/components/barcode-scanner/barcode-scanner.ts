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
import {
  BarcodeFormat,
  BrowserCodeReader,
  BrowserMultiFormatReader,
  IScannerControls,
} from '@zxing/browser';

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

  private readonly codeReader = new BrowserMultiFormatReader(undefined, {
    delayBetweenScanAttempts: 150,
    delayBetweenScanSuccess: 500,
  });

  private controls: IScannerControls | null = null;
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
      if (!window.isSecureContext) {
        throw new Error('La cámara requiere una conexión HTTPS segura.');
      }

      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('La cámara no está disponible en este navegador.');
      }

      this.codeReader.possibleFormats = [
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR,
      ];

      const video = this.videoRef.nativeElement;

      this.controls = await this.codeReader.decodeFromConstraints(
        {
          audio: false,
          video: {
            facingMode: {
              ideal: 'environment',
            },
            width: {
              ideal: 1280,
            },
            height: {
              ideal: 720,
            },
          },
        },
        video,
        (result, _error, controls) => {
          if (this.finished || !result) {
            return;
          }

          const code = result.getText().trim();

          if (!code) {
            return;
          }

          this.controls = controls;
          this.finish(code);
        },
      );

      this.loading.set(false);
      this.errorMessage.set(null);
    } catch (error) {
      this.loading.set(false);
      this.errorMessage.set(this.getCameraErrorMessage(error));
    }
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
    this.controls?.stop();
    this.controls = null;

    BrowserCodeReader.releaseAllStreams();

    const video = this.videoRef?.nativeElement;

    if (video) {
      video.pause();
      video.srcObject = null;
    }
  }

  private getCameraErrorMessage(error: unknown): string {
    if (error instanceof DOMException) {
      switch (error.name) {
        case 'NotAllowedError':
          return 'No se concedió permiso para usar la cámara. Habilita el acceso a la cámara desde la configuración del navegador.';
        case 'NotFoundError':
          return 'No se encontró una cámara disponible en este dispositivo.';
        case 'NotReadableError':
          return 'No se pudo utilizar la cámara. Verifica que otra aplicación no la esté usando.';
        case 'OverconstrainedError':
          return 'La cámara del dispositivo no admite la configuración solicitada.';
        case 'SecurityError':
          return 'El navegador bloqueó el acceso a la cámara por seguridad.';
      }
    }

    if (error instanceof Error) {
      return error.message;
    }

    return 'No fue posible iniciar el lector de código de barras.';
  }
}