import { useEffect, useRef, useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "@/components/layout/Sidebar";
import { Topbar } from "@/components/layout/Topbar";
import { cn } from "@/lib/utils";

const DEFAULT_SIDEBAR_WIDTH = 280;
const MIN_SIDEBAR_WIDTH = 224;
const MAX_SIDEBAR_WIDTH = 420;
const SIDEBAR_WIDTH_STORAGE_KEY = "sllp.sidebarWidth";

function getInitialSidebarWidth() {
  const storedWidth = window.localStorage.getItem(SIDEBAR_WIDTH_STORAGE_KEY);
  const parsedWidth = storedWidth ? Number(storedWidth) : DEFAULT_SIDEBAR_WIDTH;

  if (Number.isNaN(parsedWidth)) {
    return DEFAULT_SIDEBAR_WIDTH;
  }

  return Math.min(MAX_SIDEBAR_WIDTH, Math.max(MIN_SIDEBAR_WIDTH, parsedWidth));
}

export function AppLayout() {
  const [sidebarWidth, setSidebarWidth] = useState(getInitialSidebarWidth);
  const [isResizing, setIsResizing] = useState(false);
  const resizeStartRef = useRef({ x: 0, width: DEFAULT_SIDEBAR_WIDTH });

  useEffect(() => {
    if (!isResizing) {
      return;
    }

    const previousCursor = document.body.style.cursor;
    const previousUserSelect = document.body.style.userSelect;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";

    function handlePointerMove(event: PointerEvent) {
      const delta = event.clientX - resizeStartRef.current.x;
      const nextWidth = Math.min(
        MAX_SIDEBAR_WIDTH,
        Math.max(MIN_SIDEBAR_WIDTH, resizeStartRef.current.width + delta),
      );

      setSidebarWidth(nextWidth);
      window.localStorage.setItem(SIDEBAR_WIDTH_STORAGE_KEY, String(nextWidth));
    }

    function handlePointerUp() {
      setIsResizing(false);
    }

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);

    return () => {
      document.body.style.cursor = previousCursor;
      document.body.style.userSelect = previousUserSelect;
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
    };
  }, [isResizing]);

  function handleResizeStart(event: React.PointerEvent<HTMLButtonElement>) {
    resizeStartRef.current = {
      x: event.clientX,
      width: sidebarWidth,
    };
    setIsResizing(true);
  }

  function handleResizeKeyDown(event: React.KeyboardEvent<HTMLButtonElement>) {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
      return;
    }

    event.preventDefault();
    const direction = event.key === "ArrowRight" ? 1 : -1;
    const nextWidth = Math.min(
      MAX_SIDEBAR_WIDTH,
      Math.max(MIN_SIDEBAR_WIDTH, sidebarWidth + direction * 16),
    );
    setSidebarWidth(nextWidth);
    window.localStorage.setItem(SIDEBAR_WIDTH_STORAGE_KEY, String(nextWidth));
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="flex min-h-screen">
        <Sidebar width={sidebarWidth} />
        <button
          type="button"
          className={cn(
            "group hidden w-2 shrink-0 cursor-col-resize items-stretch justify-center bg-primary lg:flex",
            isResizing && "bg-primary",
          )}
          onPointerDown={handleResizeStart}
          onKeyDown={handleResizeKeyDown}
          aria-label="Resize sidebar"
          aria-orientation="vertical"
          aria-valuemin={MIN_SIDEBAR_WIDTH}
          aria-valuemax={MAX_SIDEBAR_WIDTH}
          aria-valuenow={Math.round(sidebarWidth)}
          role="separator"
        >
          <span
            className={cn(
              "my-3 w-px rounded-full bg-white/20 transition-colors group-hover:bg-accent",
              isResizing && "bg-accent",
            )}
          />
        </button>
        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar />
          <main className="flex-1 px-4 py-6 sm:px-6 lg:px-8">
            <div className="mx-auto w-full max-w-7xl">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}
