import React, { useState, useEffect } from 'react';

/**
 * kpedal — Complete App
 * 
 * Based on the liked version with all 6 layouts
 * + Settings and Summary screens
 * 
 * Principle: Color = Status
 * White = normal, Colored = attention needed
 */

// ════════════════════════════════════════════════════════════════════════════
// DATA & COLORS
// ════════════════════════════════════════════════════════════════════════════

const useData = () => {
  const [d, setD] = useState({ teL: 74, teR: 77, psL: 23, psR: 25, bal: 48 });
  useEffect(() => {
    const i = setInterval(() => setD(p => ({
      teL: Math.max(55, Math.min(88, p.teL + (Math.random() - 0.5) * 1.5)),
      teR: Math.max(55, Math.min(88, p.teR + (Math.random() - 0.5) * 1.5)),
      psL: Math.max(14, Math.min(32, p.psL + (Math.random() - 0.5) * 0.8)),
      psR: Math.max(14, Math.min(32, p.psR + (Math.random() - 0.5) * 0.8)),
      bal: Math.max(44, Math.min(56, p.bal + (Math.random() - 0.5) * 0.5)),
    })), 1000);
    return () => clearInterval(i);
  }, []);
  return d;
};

const C = {
  bg: '#000',
  text: '#fff',
  dim: '#666',
  muted: '#333',
  ok: '#4CAF50',
  warn: '#FFC107',
  bad: '#f44336',
};

const balanceStatus = (bal) => {
  const dev = Math.abs(50 - bal);
  if (dev <= 2) return { color: C.ok, ok: true };
  if (dev <= 5) return { color: C.warn, ok: false };
  return { color: C.bad, ok: false };
};

const teStatus = (te) => {
  if (te >= 70 && te <= 80) return { color: C.ok, ok: true };
  if (te >= 65 && te <= 85) return { color: C.warn, ok: false };
  return { color: C.bad, ok: false };
};

const psStatus = (ps) => {
  if (ps >= 20) return { color: C.ok, ok: true };
  if (ps >= 15) return { color: C.warn, ok: false };
  return { color: C.bad, ok: false };
};

// ════════════════════════════════════════════════════════════════════════════
// DATA FIELD COMPONENTS
// ════════════════════════════════════════════════════════════════════════════

const Balance = ({ value, compact = false }) => {
  const left = 100 - value;
  const status = balanceStatus(value);
  
  return (
    <div className="h-full flex flex-col" style={{ background: C.bg }}>
      <div className="flex justify-between items-center px-3 pt-2">
        <span className={`font-medium ${compact ? 'text-xs' : 'text-xs'}`} style={{ color: C.dim }}>
          BALANCE
        </span>
      </div>
      
      <div className="flex-1 flex flex-col justify-center px-3">
        <div className="flex justify-between items-baseline mb-2">
          <span 
            className={`font-bold tabular-nums ${compact ? 'text-3xl' : 'text-5xl'}`}
            style={{ color: left > 52 ? status.color : C.text }}
          >
            {left.toFixed(0)}
          </span>
          <span 
            className={`font-bold tabular-nums ${compact ? 'text-3xl' : 'text-5xl'}`}
            style={{ color: value > 52 ? status.color : C.text }}
          >
            {value.toFixed(0)}
          </span>
        </div>
        
        <div className="relative h-2 rounded-sm overflow-hidden" style={{ background: '#222' }}>
          <div 
            className="absolute left-0 top-0 bottom-0 transition-all duration-300"
            style={{ width: `${left}%`, background: left > 52 ? status.color : '#666' }}
          />
          <div 
            className="absolute right-0 top-0 bottom-0 transition-all duration-300"
            style={{ width: `${value}%`, background: value > 52 ? status.color : '#666' }}
          />
          <div 
            className="absolute top-0 bottom-0 w-0.5"
            style={{ left: '50%', background: C.text }}
          />
        </div>
        
        <div className="flex justify-between mt-1">
          <span className="text-xs" style={{ color: C.dim }}>L</span>
          <span className="text-xs" style={{ color: C.dim }}>R</span>
        </div>
      </div>
    </div>
  );
};

const TorqueEff = ({ left, right, compact = false }) => {
  const avg = (left + right) / 2;
  const status = teStatus(avg);
  
  return (
    <div className="h-full flex flex-col" style={{ background: C.bg }}>
      <div className="flex justify-between items-center px-3 pt-2">
        <span className="text-xs font-medium" style={{ color: C.dim }}>TE</span>
        <span className="text-xs font-medium" style={{ color: status.color }}>
          {avg.toFixed(0)}%
        </span>
      </div>
      
      <div className="flex-1 flex items-center justify-center gap-6 px-3">
        <div className="text-center">
          <div 
            className={`font-bold tabular-nums ${compact ? 'text-2xl' : 'text-4xl'}`}
            style={{ color: teStatus(left).color }}
          >
            {left.toFixed(0)}
          </div>
          <div className="text-xs mt-0.5" style={{ color: C.dim }}>L</div>
        </div>
        
        <div className="w-px h-8" style={{ background: C.muted }} />
        
        <div className="text-center">
          <div 
            className={`font-bold tabular-nums ${compact ? 'text-2xl' : 'text-4xl'}`}
            style={{ color: teStatus(right).color }}
          >
            {right.toFixed(0)}
          </div>
          <div className="text-xs mt-0.5" style={{ color: C.dim }}>R</div>
        </div>
      </div>
    </div>
  );
};

const PedalSmooth = ({ left, right, compact = false }) => {
  const avg = (left + right) / 2;
  const status = psStatus(avg);
  
  return (
    <div className="h-full flex flex-col" style={{ background: C.bg }}>
      <div className="flex justify-between items-center px-3 pt-2">
        <span className="text-xs font-medium" style={{ color: C.dim }}>PS</span>
        <span className="text-xs font-medium" style={{ color: status.color }}>
          {avg.toFixed(0)}%
        </span>
      </div>
      
      <div className="flex-1 flex items-center justify-center gap-6 px-3">
        <div className="text-center">
          <div 
            className={`font-bold tabular-nums ${compact ? 'text-2xl' : 'text-4xl'}`}
            style={{ color: psStatus(left).color }}
          >
            {left.toFixed(0)}
          </div>
          <div className="text-xs mt-0.5" style={{ color: C.dim }}>L</div>
        </div>
        
        <div className="w-px h-8" style={{ background: C.muted }} />
        
        <div className="text-center">
          <div 
            className={`font-bold tabular-nums ${compact ? 'text-2xl' : 'text-4xl'}`}
            style={{ color: psStatus(right).color }}
          >
            {right.toFixed(0)}
          </div>
          <div className="text-xs mt-0.5" style={{ color: C.dim }}>R</div>
        </div>
      </div>
    </div>
  );
};

const Compact = ({ label, value, statusFn }) => {
  const status = statusFn(value);
  return (
    <div className="h-full flex flex-col items-center justify-center" style={{ background: C.bg }}>
      <span className="text-xs" style={{ color: C.dim }}>{label}</span>
      <span className="text-3xl font-bold tabular-nums" style={{ color: status.color }}>
        {value.toFixed(0)}
      </span>
    </div>
  );
};

const StatusIndicator = ({ teL, teR, psL, psR, bal }) => {
  const checks = [
    teStatus((teL + teR) / 2).ok,
    psStatus((psL + psR) / 2).ok,
    balanceStatus(bal).ok,
  ];
  const allOk = checks.every(c => c);
  const problems = checks.filter(c => !c).length;
  
  return (
    <div 
      className="h-full flex flex-col items-center justify-center"
      style={{ background: allOk ? C.ok + '15' : C.warn + '15' }}
    >
      <div 
        className="text-6xl font-bold"
        style={{ color: allOk ? C.ok : problems >= 2 ? C.bad : C.warn }}
      >
        {allOk ? '✓' : '!'}
      </div>
      <div className="text-xs mt-2" style={{ color: C.dim }}>
        {allOk ? 'ALL GOOD' : `${problems} ISSUE${problems > 1 ? 'S' : ''}`}
      </div>
    </div>
  );
};

// ════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ════════════════════════════════════════════════════════════════════════════

const SettingsScreen = () => {
  const Row = ({ label, value }) => (
    <div className="flex justify-between items-center py-3 px-4" style={{ borderBottom: `1px solid ${C.muted}` }}>
      <span className="text-sm" style={{ color: C.text }}>{label}</span>
      <span className="text-sm tabular-nums" style={{ color: C.dim }}>{value}</span>
    </div>
  );
  
  const Toggle = ({ label, on }) => (
    <div className="flex justify-between items-center py-3 px-4" style={{ borderBottom: `1px solid ${C.muted}` }}>
      <span className="text-sm" style={{ color: C.text }}>{label}</span>
      <div 
        className="w-10 h-6 rounded-full relative"
        style={{ background: on ? C.ok : C.muted }}
      >
        <div 
          className="absolute top-1 w-4 h-4 rounded-full bg-white transition-all"
          style={{ left: on ? 20 : 4 }}
        />
      </div>
    </div>
  );

  return (
    <div className="h-full flex flex-col" style={{ background: C.bg }}>
      <div className="py-3 px-4" style={{ borderBottom: `1px solid ${C.muted}` }}>
        <span className="text-base font-semibold" style={{ color: C.text }}>Settings</span>
      </div>
      
      <div className="flex-1 overflow-auto">
        <div className="py-2 px-4">
          <span className="text-xs" style={{ color: C.dim }}>THRESHOLDS</span>
        </div>
        <Row label="Balance alert" value="±5%" />
        <Row label="TE optimal" value="70-80%" />
        <Row label="PS minimum" value="≥20%" />
        
        <div className="py-2 px-4 mt-2">
          <span className="text-xs" style={{ color: C.dim }}>ALERTS</span>
        </div>
        <Toggle label="Vibration" on={true} />
        <Toggle label="Sound" on={false} />
      </div>
      
      <div className="py-2 px-4" style={{ borderTop: `1px solid ${C.muted}` }}>
        <span className="text-xs" style={{ color: C.dim }}>kpedal 1.0</span>
      </div>
    </div>
  );
};

// ════════════════════════════════════════════════════════════════════════════
// SUMMARY SCREEN
// ════════════════════════════════════════════════════════════════════════════

const SummaryScreen = () => {
  const stats = {
    duration: '1:42:35',
    bal: { l: 49, r: 51 },
    te: { l: 74, r: 76 },
    ps: { l: 22, r: 24 },
    zones: { ok: 72, warn: 20, bad: 8 },
  };

  return (
    <div className="h-full flex flex-col" style={{ background: C.bg }}>
      <div className="py-3 px-4 flex justify-between items-center" style={{ borderBottom: `1px solid ${C.muted}` }}>
        <span className="text-base font-semibold" style={{ color: C.text }}>Summary</span>
        <span className="text-xs" style={{ color: C.dim }}>{stats.duration}</span>
      </div>
      
      <div className="flex-1 px-4 py-3 space-y-4 overflow-auto">
        {/* Balance */}
        <div>
          <div className="text-xs mb-2" style={{ color: C.dim }}>BALANCE</div>
          <div className="flex justify-between items-center">
            <span className="text-3xl font-bold tabular-nums" style={{ color: C.text }}>{stats.bal.l}</span>
            <div className="flex-1 mx-4 h-1.5 rounded-full overflow-hidden" style={{ background: C.muted }}>
              <div className="h-full" style={{ width: '49%', background: C.dim }} />
            </div>
            <span className="text-3xl font-bold tabular-nums" style={{ color: C.text }}>{stats.bal.r}</span>
          </div>
        </div>
        
        {/* TE & PS */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <div className="text-xs mb-2" style={{ color: C.dim }}>TE</div>
            <div className="flex justify-between">
              <span className="text-2xl font-bold tabular-nums" style={{ color: teStatus(stats.te.l).color }}>{stats.te.l}</span>
              <span className="text-2xl font-bold tabular-nums" style={{ color: teStatus(stats.te.r).color }}>{stats.te.r}</span>
            </div>
            <div className="flex justify-between text-xs" style={{ color: C.dim }}>
              <span>L</span><span>R</span>
            </div>
          </div>
          <div>
            <div className="text-xs mb-2" style={{ color: C.dim }}>PS</div>
            <div className="flex justify-between">
              <span className="text-2xl font-bold tabular-nums" style={{ color: psStatus(stats.ps.l).color }}>{stats.ps.l}</span>
              <span className="text-2xl font-bold tabular-nums" style={{ color: psStatus(stats.ps.r).color }}>{stats.ps.r}</span>
            </div>
            <div className="flex justify-between text-xs" style={{ color: C.dim }}>
              <span>L</span><span>R</span>
            </div>
          </div>
        </div>
        
        {/* Time in Zone */}
        <div>
          <div className="text-xs mb-2" style={{ color: C.dim }}>TIME IN ZONE</div>
          <div className="flex gap-0.5 h-3 rounded overflow-hidden">
            <div style={{ width: `${stats.zones.ok}%`, background: C.ok }} />
            <div style={{ width: `${stats.zones.warn}%`, background: C.warn }} />
            <div style={{ width: `${stats.zones.bad}%`, background: C.bad }} />
          </div>
          <div className="flex justify-between mt-1 text-xs">
            <span style={{ color: C.ok }}>{stats.zones.ok}%</span>
            <span style={{ color: C.warn }}>{stats.zones.warn}%</span>
            <span style={{ color: C.bad }}>{stats.zones.bad}%</span>
          </div>
        </div>
      </div>
    </div>
  );
};

// ════════════════════════════════════════════════════════════════════════════
// KAROO FRAME
// ════════════════════════════════════════════════════════════════════════════

const Karoo = ({ children, title }) => (
  <div className="flex flex-col items-center">
    <div 
      style={{
        width: 192,
        height: 320,
        background: '#111',
        borderRadius: 12,
        overflow: 'hidden',
        boxShadow: '0 10px 40px rgba(0,0,0,0.5)',
      }}
    >
      {children}
    </div>
    {title && <div className="mt-2 text-xs text-gray-500">{title}</div>}
  </div>
);

const Divider = () => <div className="h-px" style={{ background: '#222' }} />;

// ════════════════════════════════════════════════════════════════════════════
// MAIN APP
// ════════════════════════════════════════════════════════════════════════════

export default function KPedal() {
  const d = useData();
  const [tab, setTab] = useState('layouts');
  
  return (
    <div className="min-h-screen bg-neutral-900 p-6">
      {/* Header */}
      <div className="text-center mb-6">
        <h1 className="text-2xl font-bold text-white">kpedal</h1>
      </div>
      
      {/* Tabs */}
      <div className="flex justify-center gap-2 mb-6">
        {['layouts', 'settings', 'summary'].map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className="px-4 py-1.5 rounded text-xs font-medium capitalize transition-all"
            style={{
              background: tab === t ? C.ok : '#222',
              color: tab === t ? '#000' : '#666',
            }}
          >
            {t}
          </button>
        ))}
      </div>
      
      {/* Content */}
      {tab === 'layouts' && (
        <div className="flex flex-wrap justify-center gap-6">
          
          {/* Layout 1: Quick Glance */}
          <Karoo title="Quick Glance">
            <div className="h-2/5">
              <StatusIndicator 
                teL={d.teL} teR={d.teR} 
                psL={d.psL} psR={d.psR} 
                bal={d.bal} 
              />
            </div>
            <Divider />
            <div className="h-3/5">
              <Balance value={d.bal} />
            </div>
          </Karoo>
          
          {/* Layout 2: Balance Focus */}
          <Karoo title="Balance Focus">
            <div className="h-1/2">
              <Balance value={d.bal} />
            </div>
            <Divider />
            <div className="h-1/2 grid grid-cols-2">
              <Compact label="TE L" value={d.teL} statusFn={teStatus} />
              <div style={{ borderLeft: '1px solid #222' }}>
                <Compact label="TE R" value={d.teR} statusFn={teStatus} />
              </div>
            </div>
          </Karoo>
          
          {/* Layout 3: Efficiency */}
          <Karoo title="Efficiency">
            <div className="h-1/2">
              <TorqueEff left={d.teL} right={d.teR} />
            </div>
            <Divider />
            <div className="h-1/2">
              <PedalSmooth left={d.psL} right={d.psR} />
            </div>
          </Karoo>
          
          {/* Layout 4: Full Overview */}
          <Karoo title="Full Overview">
            <div className="h-full flex flex-col">
              <div className="flex-1">
                <Balance value={d.bal} compact />
              </div>
              <Divider />
              <div className="flex-1">
                <TorqueEff left={d.teL} right={d.teR} compact />
              </div>
              <Divider />
              <div className="flex-1">
                <PedalSmooth left={d.psL} right={d.psR} compact />
              </div>
            </div>
          </Karoo>
          
          {/* Layout 5: Compact Grid */}
          <Karoo title="Compact Grid">
            <div className="h-full grid grid-cols-2 grid-rows-3">
              <Compact label="TE L" value={d.teL} statusFn={teStatus} />
              <div style={{ borderLeft: '1px solid #222' }}>
                <Compact label="TE R" value={d.teR} statusFn={teStatus} />
              </div>
              <div style={{ borderTop: '1px solid #222' }}>
                <Compact label="PS L" value={d.psL} statusFn={psStatus} />
              </div>
              <div style={{ borderTop: '1px solid #222', borderLeft: '1px solid #222' }}>
                <Compact label="PS R" value={d.psR} statusFn={psStatus} />
              </div>
              <div className="col-span-2" style={{ borderTop: '1px solid #222' }}>
                <Balance value={d.bal} compact />
              </div>
            </div>
          </Karoo>
          
          {/* Layout 6: Single Field */}
          <Karoo title="Single Field">
            <div className="h-full">
              <Balance value={d.bal} />
            </div>
          </Karoo>
          
        </div>
      )}
      
      {tab === 'settings' && (
        <div className="flex justify-center">
          <Karoo>
            <SettingsScreen />
          </Karoo>
        </div>
      )}
      
      {tab === 'summary' && (
        <div className="flex justify-center">
          <Karoo>
            <SummaryScreen />
          </Karoo>
        </div>
      )}
      
      {/* Legend */}
      <div className="flex justify-center gap-6 mt-8 text-xs">
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full" style={{ background: C.ok }} />
          <span style={{ color: C.dim }}>optimal</span>
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full" style={{ background: C.warn }} />
          <span style={{ color: C.dim }}>attention</span>
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full" style={{ background: C.bad }} />
          <span style={{ color: C.dim }}>problem</span>
        </span>
      </div>
    </div>
  );
}
