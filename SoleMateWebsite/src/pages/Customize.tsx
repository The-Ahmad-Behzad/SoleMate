import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Palette, Upload, Trash2, Pencil, ShoppingBag, Send, ChevronRight, X,
} from "lucide-react";
import { useState, useEffect, useRef, useCallback } from "react";
import api from "@/config/api";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";

/* ─── Types ────────────────────────────────────────────────── */
interface Product {
  _id: string;
  name: string;
  brand: string;
  thumbnailUrl?: string;
}

interface UploadedImage {
  id: string;
  file: File;
  objectUrl: string;
  /** base64 data-url of the drawn canvas - null means no drawing yet */
  drawnDataUrl: string | null;
}

/* ─── Colour palette (mirrors mobile app) ──────────────────── */
const PALETTE = [
  { name: "Black",  hex: "#000000" },
  { name: "Brown",  hex: "#8B4513" },
  { name: "Red",    hex: "#DC143C" },
  { name: "Blue",   hex: "#0000FF" },
  { name: "Green",  hex: "#228B22" },
  { name: "Purple", hex: "#800080" },
  { name: "White",  hex: "#FFFFFF" },
  { name: "Silver", hex: "#C0C0C0" },
];

/* ═══════════════════════════════════════════════════════════════
   DrawingCanvas  –  inline over the image
═══════════════════════════════════════════════════════════════ */
function DrawingCanvas({
  image,
  onSave,
  onClose,
}: {
  image: UploadedImage;
  onSave: (dataUrl: string) => void;
  onClose: () => void;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [brushColor, setBrushColor] = useState("#DC143C");
  const [brushSize, setBrushSize] = useState(4);
  const imgRef = useRef<HTMLImageElement | null>(null);

  /* paint the background image once */
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const img = new Image();
    img.onload = () => {
      imgRef.current = img;
      canvas.width  = img.naturalWidth;
      canvas.height = img.naturalHeight;
      ctx.drawImage(img, 0, 0);

      /* restore previous drawing if exists */
      if (image.drawnDataUrl) {
        const overlay = new Image();
        overlay.onload = () => ctx.drawImage(overlay, 0, 0);
        overlay.src = image.drawnDataUrl;
      }
    };
    img.src = image.drawnDataUrl ?? image.objectUrl;
  }, [image]);

  const getPos = (e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current!;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width  / rect.width;
    const scaleY = canvas.height / rect.height;
    const src = "touches" in e ? e.touches[0] : e;
    return {
      x: (src.clientX - rect.left) * scaleX,
      y: (src.clientY - rect.top)  * scaleY,
    };
  };

  const startDraw = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault();
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx) return;
    const { x, y } = getPos(e);
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.strokeStyle = brushColor;
    ctx.lineWidth   = brushSize;
    ctx.lineCap     = "round";
    ctx.lineJoin    = "round";
    setIsDrawing(true);
  }, [brushColor, brushSize]);

  const draw = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault();
    if (!isDrawing) return;
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx) return;
    const { x, y } = getPos(e);
    ctx.lineTo(x, y);
    ctx.stroke();
  }, [isDrawing]);

  const endDraw = useCallback(() => setIsDrawing(false), []);

  const handleClear = () => {
    const canvas = canvasRef.current;
    if (!canvas || !imgRef.current) return;
    const ctx = canvas.getContext("2d")!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(imgRef.current, 0, 0);
  };

  const handleSave = () => {
    const dataUrl = canvasRef.current?.toDataURL("image/png") ?? null;
    if (dataUrl) onSave(dataUrl);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col">
      {/* Toolbar */}
      <div className="flex items-center gap-3 px-4 py-3 bg-black/80 border-b border-white/10">
        <Button variant="ghost" size="icon" onClick={onClose} className="text-white">
          <X className="h-5 w-5" />
        </Button>
        <span className="text-white font-bold flex-1">Draw on Image</span>

        {/* Brush colours */}
        <div className="flex gap-2">
          {["#DC143C","#0000FF","#000000","#FFFFFF","#228B22"].map(c => (
            <button
              key={c}
              onClick={() => setBrushColor(c)}
              className="w-7 h-7 rounded-full border-2 transition-transform hover:scale-110"
              style={{
                backgroundColor: c,
                borderColor: brushColor === c ? "#fff" : "transparent",
                transform: brushColor === c ? "scale(1.25)" : undefined,
              }}
            />
          ))}
        </div>

        {/* Brush size */}
        <input
          type="range" min={2} max={20} value={brushSize}
          onChange={e => setBrushSize(Number(e.target.value))}
          className="w-24 accent-red-500"
        />

        <Button variant="ghost" size="sm" onClick={handleClear} className="text-white">
          <Trash2 className="h-4 w-4 mr-1" /> Clear
        </Button>
        <Button size="sm" onClick={handleSave} className="bg-green-600 hover:bg-green-500 text-white">
          Save
        </Button>
      </div>

      {/* Canvas */}
      <div className="flex-1 overflow-auto flex items-center justify-center p-4">
        <canvas
          ref={canvasRef}
          className="max-w-full max-h-full cursor-crosshair touch-none"
          onMouseDown={startDraw}
          onMouseMove={draw}
          onMouseUp={endDraw}
          onMouseLeave={endDraw}
          onTouchStart={startDraw}
          onTouchMove={draw}
          onTouchEnd={endDraw}
        />
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   Main Customize Page
═══════════════════════════════════════════════════════════════ */
const Customize = () => {
  const { user, loginWithGoogle } = useAuth();

  /* catalog */
  const [catalog, setCatalog]           = useState<Product[]>([]);
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [showShoeGrid, setShowShoeGrid] = useState(false);

  /* form state */
  const [selectedShoe, setSelectedShoe] = useState<Product | null>(null);
  const [primaryIdx, setPrimaryIdx]     = useState(0);
  const [secondaryIdx, setSecondaryIdx] = useState<number | null>(null);
  const [images, setImages]             = useState<UploadedImage[]>([]);
  const [description, setDescription]  = useState("");
  const [isSending, setIsSending]       = useState(false);

  /* drawing */
  const [drawingTarget, setDrawingTarget] = useState<UploadedImage | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  /* load catalog once */
  useEffect(() => {
    api.get<Product[]>("/catalog")
      .then(r => setCatalog(r.data))
      .catch(() => toast.error("Failed to load catalog"))
      .finally(() => setLoadingCatalog(false));
  }, []);

  /* image helpers */
  const handleFilePick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const picked = Array.from(e.target.files ?? []);
    const newImgs: UploadedImage[] = picked.map(f => ({
      id: `${f.name}-${Date.now()}-${Math.random()}`,
      file: f,
      objectUrl: URL.createObjectURL(f),
      drawnDataUrl: null,
    }));
    setImages(prev => [...prev, ...newImgs]);
    e.target.value = "";
  };

  const removeImage = (id: string) =>
    setImages(prev => prev.filter(i => i.id !== id));

  const saveDrawing = (id: string, dataUrl: string) =>
    setImages(prev => prev.map(i => i.id === id ? { ...i, drawnDataUrl: dataUrl } : i));

  /* submit */
  const handleSubmit = async () => {
    if (!user) { toast.error("Please sign in first"); return; }
    if (!selectedShoe) { toast.error("Please select a base shoe"); return; }
    if (!description.trim()) { toast.error("Please describe your design"); return; }

    setIsSending(true);
    try {
      const formData = new FormData();
      formData.append("shoeId",      selectedShoe._id);
      formData.append("description", description);
      formData.append("primaryColor", PALETTE[primaryIdx].hex);
      if (secondaryIdx !== null)
        formData.append("secondaryColor", PALETTE[secondaryIdx].hex);

      /* attach images – prefer the drawn version */
      for (const img of images) {
        if (img.drawnDataUrl) {
          const res = await fetch(img.drawnDataUrl);
          const blob = await res.blob();
          formData.append("images", blob, `drawn_${img.file.name}.png`);
        } else {
          formData.append("images", img.file);
        }
      }

      await api.post("/skins/request-redesign", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      toast.success("Redesign request sent! Our sellers will get back to you.");
      setImages([]);
      setDescription("");
      setSelectedShoe(null);
      setPrimaryIdx(0);
      setSecondaryIdx(null);
    } catch (err: any) {
      toast.error(err.response?.data?.error ?? "Failed to send request");
    } finally {
      setIsSending(false);
    }
  };

  /* ─── render ─────────────────────────────────────────────── */
  return (
    <>
      {/* Drawing overlay */}
      {drawingTarget && (
        <DrawingCanvas
          image={drawingTarget}
          onSave={dataUrl => saveDrawing(drawingTarget.id, dataUrl)}
          onClose={() => setDrawingTarget(null)}
        />
      )}

      <div className="min-h-screen bg-background pt-24 pb-16">
        <div className="container mx-auto px-4 max-w-4xl">

          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="text-4xl md:text-5xl font-black tracking-tight text-foreground mb-3">
              REQUEST CUSTOM REDESIGN
            </h1>
            <p className="text-muted-foreground text-lg max-w-xl mx-auto">
              Upload reference images, sketch your vision, pick colours, and send your request to our sellers.
            </p>
          </div>

          <div className="flex flex-col gap-6">

            {/* ── 1. Shoe selector ─────────────────────────────── */}
            <Card
              className="p-6 bg-card border-none shadow-[var(--shadow-elegant)] cursor-pointer hover:ring-2 ring-accent/30 transition-all"
              onClick={() => setShowShoeGrid(s => !s)}
            >
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-accent/10 flex items-center justify-center overflow-hidden shrink-0">
                  {selectedShoe?.thumbnailUrl
                    ? <img src={selectedShoe.thumbnailUrl} className="w-full h-full object-contain" alt={selectedShoe.name} />
                    : <ShoppingBag className="h-7 w-7 text-accent" />
                  }
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-black text-xl truncate">{selectedShoe?.name ?? "Select Shoe Base"}</p>
                  <p className="text-muted-foreground text-sm">{selectedShoe?.brand ?? "Required for redesign request"}</p>
                </div>
                <ChevronRight className={`h-5 w-5 text-muted-foreground transition-transform ${showShoeGrid ? "rotate-90" : ""}`} />
              </div>

              {showShoeGrid && (
                <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-3" onClick={e => e.stopPropagation()}>
                  {loadingCatalog && <p className="col-span-4 text-center text-muted-foreground py-4">Loading…</p>}
                  {catalog.map(shoe => (
                    <button
                      key={shoe._id}
                      onClick={() => { setSelectedShoe(shoe); setShowShoeGrid(false); }}
                      className={`rounded-2xl border-2 p-2 transition-all flex flex-col items-center gap-2 ${
                        selectedShoe?._id === shoe._id ? "border-accent bg-accent/5" : "border-border hover:border-accent/40"
                      }`}
                    >
                      <div className="aspect-square w-full bg-muted rounded-xl overflow-hidden">
                        <img src={shoe.thumbnailUrl} className="w-full h-full object-contain" alt={shoe.name} />
                      </div>
                      <span className="text-xs font-bold truncate w-full text-center">{shoe.name}</span>
                    </button>
                  ))}
                </div>
              )}
            </Card>

            {/* ── 2. Reference images ──────────────────────────── */}
            <Card className="p-6 bg-card border-none shadow-[var(--shadow-elegant)]">
              <div className="flex items-center gap-3 mb-5">
                <div className="p-2 bg-accent/10 rounded-xl"><Upload className="h-5 w-5 text-accent" /></div>
                <h3 className="font-bold text-lg">Reference Images</h3>
                <Badge variant="outline" className="ml-auto text-xs">Tap image to draw</Badge>
              </div>

              {images.length === 0 ? (
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="h-28 rounded-2xl border-2 border-dashed border-border hover:border-accent/50 bg-background/40 flex flex-col items-center justify-center gap-2 cursor-pointer transition-colors"
                >
                  <Upload className="h-7 w-7 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">No images yet – click to add</p>
                </div>
              ) : (
                <div className="flex gap-3 overflow-x-auto pb-2">
                  {images.map(img => (
                    <div key={img.id} className="relative shrink-0 w-28 h-28 group">
                      <img
                        src={img.drawnDataUrl ?? img.objectUrl}
                        className="w-full h-full object-cover rounded-2xl cursor-pointer border-2 border-transparent group-hover:border-accent transition"
                        alt="Reference"
                        onClick={() => setDrawingTarget(img)}
                      />
                      {img.drawnDataUrl && (
                        <Badge className="absolute top-1 left-1 text-[9px] bg-accent text-accent-foreground px-1">Drawn</Badge>
                      )}
                      {/* pencil overlay */}
                      <div
                        onClick={() => setDrawingTarget(img)}
                        className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition rounded-2xl flex items-center justify-center cursor-pointer"
                      >
                        <Pencil className="h-6 w-6 text-white" />
                      </div>
                      {/* remove */}
                      <button
                        onClick={() => removeImage(img.id)}
                        className="absolute -top-2 -right-2 w-6 h-6 bg-destructive text-white rounded-full flex items-center justify-center hover:scale-110 transition"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <Button
                variant="outline"
                className="mt-4 w-full rounded-xl h-10"
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload className="h-4 w-4 mr-2" /> Add Images
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                className="hidden"
                accept="image/*"
                multiple
                onChange={handleFilePick}
              />
            </Card>

            {/* ── 3. Color pickers ─────────────────────────────── */}
            <Card className="p-6 bg-card border-none shadow-[var(--shadow-elegant)]">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 bg-accent/10 rounded-xl"><Palette className="h-5 w-5 text-accent" /></div>
                <h3 className="font-bold text-lg">Colours</h3>
              </div>

              {/* Primary */}
              <div className="mb-5">
                <p className="text-sm font-semibold mb-3">Primary Color <span className="text-destructive">*</span></p>
                <div className="flex flex-wrap gap-3">
                  {PALETTE.map((c, i) => (
                    <button
                      key={c.name}
                      onClick={() => setPrimaryIdx(i)}
                      className="w-10 h-10 rounded-full border-4 transition-all hover:scale-110 relative"
                      style={{
                        backgroundColor: c.hex,
                        borderColor: primaryIdx === i ? "hsl(var(--accent))" : "transparent",
                        boxShadow: primaryIdx === i ? `0 0 12px ${c.hex}88` : undefined,
                      }}
                      title={c.name}
                    >
                      {primaryIdx === i && (
                        <span className="absolute inset-0 flex items-center justify-center">
                          <span className={`text-xs font-black ${c.hex === "#FFFFFF" ? "text-black" : "text-white"}`}>✓</span>
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* Secondary */}
              <div>
                <p className="text-sm font-semibold mb-3">Secondary Color <span className="text-muted-foreground text-xs">(optional)</span></p>
                <div className="flex flex-wrap gap-3">
                  {PALETTE.map((c, i) => (
                    <button
                      key={c.name}
                      onClick={() => setSecondaryIdx(secondaryIdx === i ? null : i)}
                      className="w-10 h-10 rounded-full border-4 transition-all hover:scale-110 relative"
                      style={{
                        backgroundColor: c.hex,
                        borderColor: secondaryIdx === i ? "hsl(var(--accent))" : "transparent",
                        boxShadow: secondaryIdx === i ? `0 0 12px ${c.hex}88` : undefined,
                      }}
                      title={c.name}
                    >
                      {secondaryIdx === i && (
                        <span className="absolute inset-0 flex items-center justify-center">
                          <span className={`text-xs font-black ${c.hex === "#FFFFFF" ? "text-black" : "text-white"}`}>✓</span>
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              </div>
            </Card>

            {/* ── 4. Description ───────────────────────────────── */}
            <Card className="p-6 bg-card border-none shadow-[var(--shadow-elegant)]">
              <h3 className="font-bold text-lg mb-4">Design Instructions <span className="text-destructive">*</span></h3>
              <textarea
                className="w-full bg-background border border-border rounded-2xl p-4 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-accent resize-none"
                rows={4}
                placeholder="e.g. Please put the logo on the side and make the laces match the primary colour."
                value={description}
                onChange={e => setDescription(e.target.value)}
              />
            </Card>

            {/* ── 5. Submit ────────────────────────────────────── */}
            {!user ? (
              <Card className="p-6 bg-card border-none shadow-[var(--shadow-elegant)] flex flex-col items-center gap-4">
                <p className="font-bold text-center">Please sign in to send your redesign request</p>
                <Button onClick={loginWithGoogle} className="bg-secondary text-secondary-foreground h-12 w-full max-w-xs rounded-2xl">
                  Sign in with Google
                </Button>
              </Card>
            ) : (
              <Button
                onClick={handleSubmit}
                disabled={isSending}
                className="h-14 text-lg font-black rounded-2xl bg-secondary hover:bg-secondary/90 text-secondary-foreground shadow-xl transition-transform active:scale-95"
              >
                <Send className="h-5 w-5 mr-2" />
                {isSending ? "SENDING REQUEST…" : "SEND REQUEST"}
              </Button>
            )}

          </div>
        </div>
      </div>
    </>
  );
};

export default Customize;
