import { Routes, Route } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Dashboard from './pages/Dashboard'
import TrafficAnalyzer from './pages/TrafficAnalyzer'
import Threats from './pages/Threats'
import AttackSimulator from './pages/AttackSimulator'
import SystemStatusPage from './pages/SystemStatusPage'

export default function App() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-area">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/traffic-analyzer" element={<TrafficAnalyzer />} />
          <Route path="/threats" element={<Threats />} />
          <Route path="/attack-simulator" element={<AttackSimulator />} />
          <Route path="/system-status" element={<SystemStatusPage />} />
        </Routes>
      </div>
    </div>
  )
}
