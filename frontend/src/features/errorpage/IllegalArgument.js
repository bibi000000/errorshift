import NavBar from '../../common/navbar/NavBar'
import Footer from '../../common/footer/Footer'
import SideBar from '../../common/sidebar/SideBar'

function IllegalArgument(){
    return(
        <div>
            <NavBar></NavBar>
            <div className="overall-screen">
                <div>
                    <SideBar></SideBar>
                </div>
                <div style={{height:500, marginTop:80}}>
                    <h1>IllegalArgument</h1>
                </div>
            </div>
            <Footer></Footer>
        </div>
    )
}
export default IllegalArgument;