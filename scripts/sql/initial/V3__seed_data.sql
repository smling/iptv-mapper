INSERT INTO data_source(type, url, label, priority) VALUES
  ('M3U','https://iptv-org.github.io/iptv/index.m3u','IPTV.org master playlist',50)
ON CONFLICT DO NOTHING;