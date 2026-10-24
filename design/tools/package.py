"""Create a portable identity kit, omitting dependencies and the archive itself."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root = Path(__file__).resolve().parents[1]
destination = root / "pacemaker-identity-kit.zip"
with ZipFile(destination, "w", ZIP_DEFLATED, compresslevel=9) as archive:
    for item in sorted(root.rglob("*")):
        if not item.is_file() or item == destination:
            continue
        relative = item.relative_to(root)
        if any(part in {"node_modules", ".cache", ".DS_Store", "__pycache__"} for part in relative.parts):
            continue
        archive.write(item, Path("design") / relative)
print(f"Packaged {destination.name}: {destination.stat().st_size:,} bytes")
